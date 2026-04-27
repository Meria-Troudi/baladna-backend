import json
import os
from datetime import datetime

import joblib
import mysql.connector
import pandas as pd
from flask import Flask, jsonify, request
from flask_cors import CORS
from textblob import TextBlob

app = Flask(__name__)
CORS(app)

MODEL_DIR = os.path.join(os.path.dirname(__file__), "model")

# Load recommendation models
try:
    model = joblib.load(os.path.join(MODEL_DIR, "model.pkl"))
    encoders = joblib.load(os.path.join(MODEL_DIR, "encoders.pkl"))
    with open(os.path.join(MODEL_DIR, "feature_names.json")) as f:
        FEATURES = json.load(f)
    with open(os.path.join(MODEL_DIR, "label_encoder_classes.json")) as f:
        LE_CLASSES = json.load(f)
    print("✅ Recommendation models loaded")
except Exception as e:
    print(f"⚠️ Recommendation models not loaded: {e}")

# Load host analytics models
try:
    host_fill_model = joblib.load(os.path.join(MODEL_DIR, "host_fill_rate_model.pkl"))
    host_rev_model = joblib.load(os.path.join(MODEL_DIR, "host_revenue_model.pkl"))
    host_rating_model = joblib.load(os.path.join(MODEL_DIR, "host_rating_model.pkl"))
    host_category_encoder = joblib.load(os.path.join(MODEL_DIR, "host_category_encoder.pkl"))
    with open(os.path.join(MODEL_DIR, "host_feature_names.json")) as f:
        HOST_FEATURES = json.load(f)
    print("✅ Host analytics models loaded")
except Exception as e:
    print(f"⚠️ Host analytics models not loaded: {e}")

DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "baladna")
}

def get_conn():
    return mysql.connector.connect(**DB_CONFIG)

# ==================== EXISTING RECOMMENDATION ENDPOINTS ====================

@app.route("/recommend/<int:user_id>")
def recommend(user_id):
    try:
        conn = get_conn()
        # Get user features
        user_features = get_user_features(user_id, conn)
        # Get all upcoming events
        events = pd.read_sql("SELECT id FROM event WHERE status = 'UPCOMING'", conn)
        conn.close()
        
        results = []
        for _, ev in events.iterrows():
            event_id = int(ev['id'])
            event_stats = get_event_stats(event_id)
            features = build_features(event_stats, user_features)
            score = float(model.predict([features])[0])
            results.append({"eventId": event_id, "score": score})
        
        results.sort(key=lambda x: x['score'], reverse=True)
        return jsonify({"recommendations": results, "total": len(results)})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def get_user_features(user_id, conn):
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*), AVG(price) FROM event_reservation WHERE user_id = %s AND status = 'CONFIRMED'", (user_id,))
    row = cur.fetchone()
    cur.execute("SELECT category FROM event e JOIN event_reservation er ON e.id = er.event_id WHERE er.user_id = %s GROUP BY category ORDER BY COUNT(*) DESC LIMIT 1", (user_id,))
    fav = cur.fetchone()
    cur.close()
    return {
        "user_total_bookings": int(row[0] or 0),
        "user_avg_price_paid": float(row[1] or 0),
        "user_favorite_category": fav[0] if fav else "NONE"
    }

def get_event_stats(event_id):
    conn = get_conn()
    ev = pd.read_sql(f"SELECT * FROM event WHERE id = {event_id}", conn).iloc[0]
    conn.close()
    return {
        "event_category": ev['category'],
        "event_price": float(ev['price']),
        "event_capacity": int(ev['capacity']),
        "event_hour": pd.to_datetime(ev['start_at']).hour,
        "event_is_weekend": 1 if pd.to_datetime(ev['start_at']).dayofweek >= 5 else 0,
        "event_days_until": (pd.to_datetime(ev['start_at']) - datetime.now()).days
    }

def build_features(event_stats, user_features):
    features = []
    for f in FEATURES:
        if f in event_stats:
            features.append(event_stats[f])
        elif f in user_features:
            features.append(user_features[f])
        else:
            features.append(0)
    return features

@app.route("/explain/<int:user_id>/<int:event_id>")
def explain(user_id, event_id):
    return jsonify({"reasons": ["Based on your booking history", "Similar category preference"]})

@app.route("/trending")
def trending():
    conn = get_conn()
    events = pd.read_sql("SELECT id, title FROM event WHERE status = 'UPCOMING' ORDER BY start_at LIMIT 10", conn)
    conn.close()
    return jsonify(events.to_dict(orient='records'))

@app.route("/health")
def health():
    return jsonify({"status": "ok", "features": len(FEATURES) if 'FEATURES' in globals() else 0})

# ==================== NEW HOST ANALYTICS ENDPOINTS ====================

def get_host_features(host_id):
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("""
        SELECT COUNT(DISTINCT e.id), AVG(e.price), AVG(e.capacity), COUNT(er.id),
               AVG(CASE WHEN er.status='CONFIRMED' THEN er.persons_count ELSE 0 END)
        FROM event e
        LEFT JOIN event_reservation er ON e.id = er.event_id
        WHERE e.created_by_user_id = %s AND e.end_at < NOW()
    """, (host_id,))
    row = cur.fetchone()
    cur.execute("""
        SELECT AVG(rev.rating)
        FROM event_review rev
        JOIN event e ON rev.event_id = e.id
        WHERE e.created_by_user_id = %s AND e.end_at < NOW()
    """, (host_id,))
    avg_rating = cur.fetchone()[0] or 0.0
    cur.close()
    conn.close()
    return {
        "host_total_events": int(row[0] or 0),
        "host_avg_event_price": round(row[1] or 0, 2),
        "host_avg_capacity": int(row[2] or 0),
        "host_total_bookings": int(row[3] or 0),
        "host_avg_confirmed_per_event": round(row[4] or 0, 2),
        "host_avg_rating": round(avg_rating, 2)
    }

def build_host_features(event_id):
    conn = get_conn()
    ev = pd.read_sql(f"SELECT * FROM event WHERE id = {event_id}", conn).iloc[0]
    conn.close()
    start = pd.to_datetime(ev['start_at'])
    host_feat = get_host_features(ev['created_by_user_id'])
    row = {
        "category": host_category_encoder.transform([str(ev['category'])])[0],
        "price": float(ev['price']),
        "capacity": int(ev['capacity']),
        "day_of_week": start.dayofweek,
        "hour": start.hour,
        "is_weekend": 1 if start.dayofweek >= 5 else 0,
        "duration_hours": (pd.to_datetime(ev['end_at']) - start).total_seconds() / 3600,
        "latitude": float(ev['latitude']) if ev['latitude'] else 0.0,
        "longitude": float(ev['longitude']) if ev['longitude'] else 0.0,
        **host_feat,
        "media_count": 0
    }
    df = pd.DataFrame([row])[HOST_FEATURES]
    return df

@app.route("/host/predict/<int:event_id>")
def host_predict(event_id):
    try:
        X = build_host_features(event_id)
        fill_rate = float(host_fill_model.predict(X)[0])
        revenue = float(host_rev_model.predict(X)[0])
        rating = float(host_rating_model.predict(X)[0])
        
        conn = get_conn()
        ev = pd.read_sql(f"SELECT capacity, price FROM event WHERE id = {event_id}", conn).iloc[0]
        conn.close()
        
        expected_bookings = int(fill_rate * int(ev['capacity']))
        
        if fill_rate >= 0.8:
            perf = "Excellent"
            color = "success"
        elif fill_rate >= 0.6:
            perf = "Good"
            color = "info"
        elif fill_rate >= 0.4:
            perf = "Moderate"
            color = "warning"
        else:
            perf = "Needs Improvement"
            color = "danger"
            
        return jsonify({
            "eventId": event_id,
            "predictions": {
                "fillRate": round(fill_rate * 100, 1),
                "expectedBookings": expected_bookings,
                "expectedRevenue": round(revenue, 2),
                "expectedRating": round(rating, 2)
            },
            "performance": {"level": perf, "color": color},
            "recommendations": [
                {"type": "info", "title": "Price check", "message": f"Current price ${float(ev['price'])}"}
            ]
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/sentiment/analyze", methods=["POST"])
def analyze_sentiment():
    data = request.get_json()
    text = data.get("text", "")
    blob = TextBlob(text)
    score = round(blob.sentiment.polarity, 3)
    if score > 0.3:
        label, emoji = "Positive", "😊"
    elif score < -0.3:
        label, emoji = "Negative", "😞"
    else:
        label, emoji = "Neutral", "😐"
    return jsonify({"sentimentScore": score, "sentimentLabel": label, "emoji": emoji})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)