import json, os, joblib, mysql.connector, pandas as pd
from datetime import datetime
from flask import Flask, jsonify, request
from flask_cors import CORS
from collections import Counter
import numpy as np

app = Flask(__name__)
CORS(app)

MODEL_DIR = os.path.join(os.path.dirname(__file__), "model")
model = joblib.load(os.path.join(MODEL_DIR, "model.pkl"))
encoders = joblib.load(os.path.join(MODEL_DIR, "encoders.pkl"))
scaler = joblib.load(os.path.join(MODEL_DIR, "scaler.pkl"))
with open(os.path.join(MODEL_DIR, "feature_names.json")) as f:
    FEATURES = json.load(f)
with open(os.path.join(MODEL_DIR, "label_encoder_classes.json")) as f:
    LE_CLASSES = json.load(f)
NUMERIC_COLS = ["event_price", "event_popularity", "context_price_ratio", "context_hour_diff"]

DB_CONFIG = {"host": os.getenv("DB_HOST", "localhost"), "user": os.getenv("DB_USER", "root"),
             "password": os.getenv("DB_PASSWORD", ""), "database": os.getenv("DB_NAME", "baladna")}

def get_conn():
    return mysql.connector.connect(**DB_CONFIG)

def safe_encode(col, value):
    le = encoders[col]
    classes = LE_CLASSES[col]
    value = str(value if value is not None else "NONE")
    if value not in classes:
        value = "NONE" if "NONE" in classes else classes[0]
    return int(le.transform([value])[0])

def get_user_features(conn, user_id):
    q = """
        SELECT e.category, er.total_price, er.persons_count, e.start_at, e.created_by_user_id, e.price
        FROM event_reservation er
        JOIN event e ON er.event_id = e.id
        WHERE er.user_id = %s AND er.status = 'CONFIRMED' AND er.payment_status = 'PAID'
    """
    df = pd.read_sql(q, conn, params=(user_id,))
    if df.empty:
        return {"user_total_bookings": 0, "user_avg_price_paid": 0.0, "user_avg_group_size": 1.0,
                "user_favorite_category": "NONE", "user_preferred_hour": 12, "cat_counts": {}, "host_counts": {},
                "user_price_min": 0, "user_price_max": 0, "user_price_std": 0.0}
    cat_counts = Counter(df["category"])
    host_counts = Counter(df["created_by_user_id"])
    preferred_hour = int(pd.to_datetime(df["start_at"]).dt.hour.mode()[0])
    prices = df["price"].tolist()
    return {
        "user_total_bookings": len(df), "user_avg_price_paid": float(df["total_price"].mean()),
        "user_avg_group_size": float(df["persons_count"].mean()), "user_favorite_category": cat_counts.most_common(1)[0][0],
        "user_preferred_hour": preferred_hour, "cat_counts": dict(cat_counts), "host_counts": dict(host_counts),
        "user_price_min": min(prices) if prices else 0, "user_price_max": max(prices) if prices else 0,
        "user_price_std": np.std(prices) if len(prices) > 1 else 0.0
    }

def get_event_stats(conn):
    q = """
        SELECT e.id AS event_id, COALESCE(COUNT(DISTINCT er.id), 0) AS event_popularity,
               COALESCE(AVG(rv.rating), 0) AS event_avg_rating,
               COALESCE(AVG(rv.sentiment_score), 0) AS event_avg_sentiment
        FROM event e
        LEFT JOIN event_reservation er ON er.event_id = e.id AND er.status <> 'CANCELLED'
        LEFT JOIN event_review rv ON rv.event_id = e.id
        WHERE e.status = 'UPCOMING'
        GROUP BY e.id
    """
    df = pd.read_sql(q, conn)
    return df.set_index("event_id").to_dict(orient="index")

def build_features(conn, user_id):
    events = pd.read_sql("""
        SELECT e.id, e.category, e.price, e.capacity, e.booked_seats, e.start_at, e.end_at, e.created_by_user_id
        FROM event e WHERE e.status = 'UPCOMING' ORDER BY e.id
    """, conn)
    
    # Candidate filtering - very important!
    if not events.empty:
        user = get_user_features(conn, user_id)
        # Cold-start: if the user has never paid for an event yet, skip the price
        # ceiling (otherwise the filter becomes price <= 0 and we'd recommend
        # nothing). For returning users, cap at 2x their average paid price.
        if user["user_avg_price_paid"] > 0:
            events = events[events["price"] <= user["user_avg_price_paid"] * 2]
        events = events[pd.to_datetime(events["start_at"]) > datetime.now()]
        
        # Filter out events user already reserved
        reserved_q = """
            SELECT event_id FROM event_reservation 
            WHERE user_id = %s AND status IN ('CONFIRMED', 'WAITLISTED', 'PAID')
        """
        reserved_df = pd.read_sql(reserved_q, conn, params=(user_id,))
        if not reserved_df.empty:
            reserved_ids = set(reserved_df["event_id"])
            events = events[~events["id"].isin(reserved_ids)]
    if events.empty:
        return pd.DataFrame(), [], events
    user = get_user_features(conn, user_id)
    stats = get_event_stats(conn)
    now = datetime.now()
    rows, event_ids = [], []
    for _, ev in events.iterrows():
        ev_stats = stats.get(int(ev["id"]), {"event_popularity": 0, "event_avg_rating": 0.0, "event_avg_sentiment": 0.0})
        start_dt = pd.to_datetime(ev["start_at"])
        event_hour = start_dt.hour
        price_ratio = ev["price"] / user["user_avg_price_paid"] if user["user_avg_price_paid"] > 0 else 1.0
        category_match = int(ev["category"] == user["user_favorite_category"])
        cat_novelty = int(ev["category"] not in user["cat_counts"])
        same_category = int(user["cat_counts"].get(ev["category"], 0))
        same_host = int(user["host_counts"].get(ev["created_by_user_id"], 0))
        price_within_range = int(user["user_price_min"] <= ev["price"] <= user["user_price_max"])
        price_within_std = int(user["user_price_std"] > 0 and (user["user_price_min"] + user["user_price_std"] >= ev["price"] >= user["user_price_min"] - user["user_price_std"]))
        rows.append({
            "user_total_bookings": user["user_total_bookings"],
            "user_avg_price_paid": user["user_avg_price_paid"],
            "user_avg_group_size": user["user_avg_group_size"],
            "user_favorite_category": safe_encode("user_favorite_category", user["user_favorite_category"]),
            "user_preferred_hour": user["user_preferred_hour"],
            "event_category": safe_encode("event_category", ev["category"]),
            "event_price": float(ev["price"]),
            "event_capacity": int(ev["capacity"]),
            "event_hour": event_hour,
            "event_is_weekend": int(start_dt.weekday() >= 5),
            "event_days_until": max((start_dt - now).days, 0),
            "event_availability": round(1.0 - (float(ev["booked_seats"] or 0) / max(int(ev["capacity"]), 1)), 4),
            "event_popularity": int(ev_stats["event_popularity"]),
            "event_avg_rating": float(ev_stats["event_avg_rating"]),
            "event_avg_sentiment": float(ev_stats["event_avg_sentiment"]),
            "interaction_same_category": same_category,
            "interaction_same_host": same_host,
            "context_price_ratio": price_ratio,
            "context_category_match": category_match,
            "context_cat_novelty": cat_novelty,
            "context_hour_diff": abs(event_hour - user["user_preferred_hour"]),
            "context_price_within_range": price_within_range,
            "context_price_within_std": price_within_std
        })
        event_ids.append(int(ev["id"]))
    df = pd.DataFrame(rows)
    for col in FEATURES:
        if col not in df.columns:
            df[col] = 0
    return df[FEATURES], event_ids, events

@app.route("/recommend/<int:user_id>")
def recommend(user_id):
    conn = get_conn()
    X, event_ids, events = build_features(conn, user_id)
    conn.close()
    if X.empty:
        return jsonify({"recommendations": [], "message": "No upcoming events"})
    scores = model.predict(X)
    results = [{"eventId": eid, "score": round(float(score), 4)} for eid, score in zip(event_ids, scores)]
    results.sort(key=lambda x: x["score"], reverse=True)
    return jsonify({"recommendations": results[:20], "total": len(results)})

@app.route("/explain/<int:user_id>/<int:event_id>")
def explain(user_id, event_id):
    conn = get_conn()
    X, event_ids, events = build_features(conn, user_id)
    conn.close()
    if event_id not in event_ids:
        return jsonify({"error": "Event not found"}), 404
    idx = event_ids.index(event_id)
    row = X.iloc[idx]
    ev = events[events["id"] == event_id].iloc[0]
    reasons = []
    if row["context_category_match"] == 1:
        reasons.append(f"Matches your favourite category: {ev['category']}")
    if row["context_price_ratio"] <= 1.2:
        reasons.append(f"Fits your usual budget: {ev['price']} TND")
    if row["interaction_same_category"] > 0:
        reasons.append(f"You've liked {ev['category']} events before")
    if row["event_is_weekend"] == 1:
        reasons.append("Weekend event")
    if row["context_cat_novelty"] == 1:
        reasons.append(f"New category for you: {ev['category']}")
    if row["event_popularity"] > 0:
        reasons.append("Popular among tourists")
    if row["context_price_within_range"] == 1:
        reasons.append("Inside your usual price range")
    if not reasons:
        reasons.append("Trending in your region")
    return jsonify({"eventId": event_id, "userId": user_id, "reasons": reasons})

# ---------------------- HOST ANALYTICS ----------------------
@app.route("/host/fill-rate-prediction/<int:event_id>")
def fill_rate_prediction(event_id):
    """Predict fill rate (0-1) for an event"""
    conn = get_conn()
    df = pd.read_sql("SELECT price, category, capacity, start_at, created_by_user_id FROM event WHERE id = %s", conn, params=(event_id,))
    if df.empty:
        return jsonify({"error": "Event not found"}), 404
    ev = df.iloc[0]
    # Simple regression model (would be trained separately)
    # For demo, use heuristic: based on days until start and category popularity
    days_until = max((pd.to_datetime(ev["start_at"]) - datetime.now()).days, 0)
    base_fill = 0.3 + 0.5 * np.exp(-days_until/10)
    category_popularity = {"MUSIC":0.8, "FOOD":0.7, "SPORT":0.6}.get(ev["category"], 0.5)
    predicted = min(0.95, base_fill * category_popularity)
    return jsonify({"eventId": event_id, "predictedFillRate": round(predicted, 2)})

@app.route("/host/revenue-forecast/<int:event_id>")
def revenue_forecast(event_id):
    conn = get_conn()
    df = pd.read_sql("SELECT price, booked_seats FROM event WHERE id = %s", conn, params=(event_id,))
    if df.empty:
        return jsonify({"error": "Event not found"}), 404
    ev = df.iloc[0]
    fill_rate = fill_rate_prediction(event_id).json["predictedFillRate"]
    capacity = pd.read_sql("SELECT capacity FROM event WHERE id = %s", conn, params=(event_id,)).iloc[0]["capacity"]
    predicted_booked = int(capacity * fill_rate)
    current_revenue = ev["price"] * (ev["booked_seats"] or 0)
    forecast_revenue = ev["price"] * predicted_booked
    return jsonify({"eventId": event_id, "currentRevenue": current_revenue, "forecastRevenue": forecast_revenue})

@app.route("/host/rating-prediction/<int:event_id>")
def rating_prediction(event_id):
    """Predict average rating (1-5) based on event features"""
    conn = get_conn()
    df = pd.read_sql("SELECT price, category, start_at, created_by_user_id FROM event WHERE id = %s", conn, params=(event_id,))
    if df.empty:
        return jsonify({"error": "Event not found"}), 404
    ev = df.iloc[0]
    # Heuristic: higher price = higher expectation, category baseline
    baseline = {"MUSIC":4.2, "FOOD":4.0, "SPORT":3.8}.get(ev["category"], 3.5)
    price_adjust = min(0.5, max(-0.5, (ev["price"] - 30) / 100))
    predicted = round(min(5, max(1, baseline + price_adjust)), 1)
    return jsonify({"eventId": event_id, "predictedRating": predicted})

@app.route("/host/actionable-tips/<int:event_id>")
def actionable_tips(event_id):
    conn = get_conn()
    df = pd.read_sql("SELECT title, price, booked_seats, capacity, start_at FROM event WHERE id = %s", conn, params=(event_id,))
    if df.empty:
        return jsonify({"error": "Event not found"}), 404
    ev = df.iloc[0]
    tips = []
    fill_rate = (ev["booked_seats"] or 0) / max(ev["capacity"], 1)
    days_until = max((ev["start_at"] - datetime.now()).days, 0)
    if fill_rate < 0.3 and days_until < 7:
        tips.append("Low fill rate – offer a 15% discount to boost bookings")
    if ev["price"] > 80 and fill_rate < 0.4:
        tips.append("Price may be high – consider reducing by 10-20%")
    if days_until > 30:
        tips.append("Event is far in the future – start promoting on social media")
    if not tips:
        tips.append("Your event is on track! Monitor reviews after completion")
    return jsonify({"eventId": event_id, "tips": tips})

# ---------------------- SENTIMENT ANALYSIS ----------------------
# Lazy-loaded so the service can boot without the heavy transformers/torch
# dependency installed. The endpoint only fails (with a clear message) if
# someone actually calls /sentiment/analyze without those packages available.
_sentiment_pipeline = None

def _get_sentiment_pipeline():
    global _sentiment_pipeline
    if _sentiment_pipeline is None:
        from transformers import pipeline  # pip install transformers torch
        _sentiment_pipeline = pipeline(
            "sentiment-analysis",
            model="cardiffnlp/twitter-roberta-base-sentiment",
            device=-1,
        )
    return _sentiment_pipeline

@app.route("/sentiment/analyze", methods=["POST"])
def analyze_sentiment():
    data = request.get_json()
    text = data.get("text", "")
    if not text:
        return jsonify({"error": "No text provided"}), 400
    try:
        pipe = _get_sentiment_pipeline()
    except Exception as e:
        return jsonify({"error": f"sentiment model unavailable: {e}"}), 503
    result = pipe(text[:512])[0]
    label = result["label"]  # LABEL_0 (negative), LABEL_1 (neutral), LABEL_2 (positive)
    sentiment_score = {"LABEL_0": -1, "LABEL_1": 0, "LABEL_2": 1}[label]
    return jsonify({"sentiment": label, "score": sentiment_score, "confidence": result["score"]})

# ---------------------- TRENDING ----------------------
@app.route("/trending")
def trending():
    conn = get_conn()
    df = pd.read_sql("""
        SELECT
            e.id            AS id,
            e.id            AS eventId,
            e.title         AS title,
            e.description   AS description,
            e.category      AS category,
            e.price         AS price,
            e.capacity      AS capacity,
            e.booked_seats  AS bookedSeats,
            e.start_at      AS startAt,
            e.end_at        AS endAt,
            e.location      AS location,
            e.latitude      AS latitude,
            e.longitude     AS longitude,
            e.status        AS status,
            COUNT(er.id)    AS bookings
        FROM event e
        LEFT JOIN event_reservation er
               ON er.event_id = e.id AND er.status <> 'CANCELLED'
        WHERE e.status = 'UPCOMING'
        GROUP BY e.id, e.title, e.description, e.category, e.price, e.capacity,
                 e.booked_seats, e.start_at, e.end_at, e.location, e.latitude,
                 e.longitude, e.status
        ORDER BY bookings DESC, e.start_at ASC
        LIMIT 12
    """, conn)
    conn.close()
    # pandas serializes datetimes/Decimals oddly via to_dict; coerce to JSON-safe types.
    if not df.empty:
        for col in ("startAt", "endAt"):
            if col in df.columns:
                df[col] = pd.to_datetime(df[col]).dt.strftime("%Y-%m-%dT%H:%M:%S")
        for col in ("price", "latitude", "longitude"):
            if col in df.columns:
                df[col] = df[col].astype(float)
        for col in ("id", "eventId", "capacity", "bookedSeats", "bookings"):
            if col in df.columns:
                df[col] = df[col].fillna(0).astype(int)
    return jsonify(df.to_dict(orient="records"))

@app.route("/health")
def health():
    return jsonify({"status": "ok", "features": len(FEATURES)})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)