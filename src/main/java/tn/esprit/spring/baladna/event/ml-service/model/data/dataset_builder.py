import pandas as pd
import mysql.connector
from datetime import datetime

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "baladna"
}

class DatasetBuilder:
    def __init__(self):
        self.conn = mysql.connector.connect(**DB_CONFIG)

    # ==================== RECOMMENDATION DATASET ====================
    def build_recommendation_dataset(self):
        print("Building recommendation dataset...")
        # ⚠️ KEEP YOUR ORIGINAL IMPLEMENTATION HERE
        print("⚠️ Using existing recommendation builder (not modified)")
        pass

    # ==================== HOST ANALYTICS DATASET ====================
    def build_host_analytics_dataset(self):
        print("\nBuilding host analytics dataset (leakage-free)...")

        events = pd.read_sql("""
            SELECT 
                e.id, e.created_by_user_id, e.category, e.price, e.capacity,
                e.start_at, e.end_at, e.latitude, e.longitude,
                (SELECT COUNT(*) FROM event_media WHERE event_id = e.id) as media_count
            FROM event e
            WHERE e.status IN ('FINISHED', 'UPCOMING', 'ONGOING')
        """, self.conn)

        rows = []

        for _, ev in events.iterrows():
            try:
                start = pd.to_datetime(ev['start_at'])

                host_features = self._get_host_features_historical(
                    ev['created_by_user_id'], start
                )

                perf = self._get_event_performance_historical(
                    ev['id'], start, ev['capacity'], ev['price']
                )

                row = {
                    # identifiers
                    "event_id": ev['id'],
                    "host_id": ev['created_by_user_id'],

                    # event features
                    "category": str(ev['category']),
                    "price": float(ev['price'] or 0),
                    "capacity": int(ev['capacity'] or 0),

                    "day_of_week": start.dayofweek,
                    "hour": start.hour,
                    "is_weekend": 1 if start.dayofweek >= 5 else 0,
                    "duration_hours": (
                        (pd.to_datetime(ev['end_at']) - start).total_seconds() / 3600
                        if ev['end_at'] else 0
                    ),

                    "latitude": float(ev['latitude'] or 0),
                    "longitude": float(ev['longitude'] or 0),
                    "media_count": int(ev['media_count'] or 0),

                    # host features
                    **host_features,

                    # targets
                    **perf
                }

                rows.append(row)

            except Exception as e:
                print(f"⚠️ Skipping event {ev['id']} due to error: {e}")

        df = pd.DataFrame(rows)

        # ------------------ CLEANING ------------------
        df.fillna(0, inplace=True)

        # optional: remove useless rows
        df = df[df['capacity'] > 0]

        df.to_csv("dataset_host_analytics.csv", index=False)

        print(f"✅ Dataset created: {df.shape}")
        return df

    # ==================== HOST FEATURES ====================
    def _get_host_features_historical(self, host_id, before_date):
        cur = self.conn.cursor()

        cur.execute("""
            SELECT
                COUNT(DISTINCT e.id),
                AVG(e.price),
                AVG(e.capacity),
                COUNT(er.id),
                AVG(CASE WHEN er.status='CONFIRMED' THEN er.persons_count ELSE 0 END)
            FROM event e
            LEFT JOIN event_reservation er ON e.id = er.event_id
            WHERE e.created_by_user_id = %s
              AND e.end_at < %s
        """, (host_id, before_date))

        row = cur.fetchone()

        cur.execute("""
            SELECT AVG(rev.rating)
            FROM event_review rev
            JOIN event e ON rev.event_id = e.id
            WHERE e.created_by_user_id = %s
              AND e.end_at < %s
        """, (host_id, before_date))

        avg_rating = cur.fetchone()[0] or 0

        cur.close()

        return {
            "host_total_events": int(row[0] or 0),
            "host_avg_event_price": float(row[1] or 0),
            "host_avg_capacity": float(row[2] or 0),
            "host_total_bookings": int(row[3] or 0),
            "host_avg_confirmed_per_event": float(row[4] or 0),
            "host_avg_rating": float(avg_rating)
        }

    # ==================== PERFORMANCE (LEAKAGE-FREE) ====================
    def _get_event_performance_historical(self, event_id, before_date, capacity, price):
        cur = self.conn.cursor()

        cur.execute("""
            SELECT
                COALESCE(SUM(er.persons_count), 0),
                COUNT(er.id),
                AVG(rev.rating),
                AVG(rev.sentiment_score),
                COUNT(rev.id)
            FROM event_reservation er
            LEFT JOIN event_review rev ON er.id = rev.reservation_id
            WHERE er.event_id = %s
              AND er.created_at < %s
              AND er.status = 'CONFIRMED'
              AND er.payment_status = 'PAID'
        """, (event_id, before_date))

        result = cur.fetchone()
        cur.close()

        confirmed_seats = int(result[0] or 0)
        total_reservations = int(result[1] or 0)
        avg_rating = float(result[2] or 0)
        avg_sentiment = float(result[3] or 0)
        review_count = int(result[4] or 0)

        fill_rate = confirmed_seats / capacity if capacity > 0 else 0
        expected_revenue = confirmed_seats * price

        return {
            "fill_rate": fill_rate,
            "expected_revenue": expected_revenue,
            "avg_rating": avg_rating,
            "avg_sentiment": avg_sentiment,
            "total_reservations": total_reservations,
            "confirmed_seats": confirmed_seats,
            "review_count": review_count
        }

    # ==================== MAIN ====================
    def build_all(self):
        self.build_recommendation_dataset()
        self.build_host_analytics_dataset()
        self.conn.close()


if __name__ == "__main__":
    DatasetBuilder().build_all()