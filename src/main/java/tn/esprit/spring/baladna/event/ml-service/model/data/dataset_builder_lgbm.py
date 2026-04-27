# dataset_builder.py
import random
from collections import Counter, defaultdict
from datetime import datetime
import pandas as pd
import mysql.connector

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "baladna"
}

TARGET_TOURISTS = 5

# Synthetic demo tourists if your DB has fewer than 5 tourists
SYNTHETIC_PROFILES = {
    1001: {
        "name": "DemoA",
        "preferred": ["MUSIC", "NIGHTLIFE", "FESTIVAL"],
        "budget": (30, 90),
        "preferred_hour": 21,
        "avg_group_size": 2.0
    },
    1002: {
        "name": "DemoB",
        "preferred": ["SPORT", "OUTDOOR", "TOUR"],
        "budget": (15, 70),
        "preferred_hour": 10,
        "avg_group_size": 3.0
    }
}

class DatasetBuilder:
    def __init__(self):
        self.conn = mysql.connector.connect(**DB_CONFIG)

    def load_real_tourists(self):
        q = """
            SELECT id
            FROM user
            WHERE role = 'TOURIST'
            ORDER BY id
        """
        df = pd.read_sql(q, self.conn)
        return df["id"].tolist()

    def load_events(self):
        q = """
            SELECT
                e.id,
                e.title,
                e.category,
                e.price,
                e.capacity,
                e.booked_seats,
                e.start_at,
                e.end_at,
                e.latitude,
                e.longitude,
                e.created_by_user_id
            FROM event e
            WHERE e.status = 'UPCOMING'
            ORDER BY e.id
        """
        return pd.read_sql(q, self.conn)

    def load_reservations(self):
        q = """
            SELECT
                er.id,
                er.user_id,
                er.event_id,
                er.persons_count,
                er.total_price,
                er.status,
                er.payment_status,
                er.created_at
            FROM event_reservation er
            ORDER BY er.id
        """
        return pd.read_sql(q, self.conn)

    def load_event_stats(self):
        q = """
            SELECT
                e.id AS event_id,
                COALESCE(COUNT(DISTINCT er.id), 0) AS event_popularity,
                COALESCE(AVG(rv.rating), 0) AS event_avg_rating,
                COALESCE(AVG(rv.sentiment_score), 0) AS event_avg_sentiment
            FROM event e
            LEFT JOIN event_reservation er
                   ON er.event_id = e.id
                  AND er.status <> 'CANCELLED'
            LEFT JOIN event_review rv
                   ON rv.event_id = e.id
            GROUP BY e.id
        """
        df = pd.read_sql(q, self.conn)
        return df.set_index("event_id").to_dict(orient="index")

    def _real_user_features(self, user_id, reservations, events):
        user_res = reservations[
            (reservations["user_id"] == user_id) &
            (reservations["status"] == "CONFIRMED") &
            (reservations["payment_status"] == "PAID")
        ]

        if user_res.empty:
            return {
                "user_total_bookings": 0,
                "user_avg_price_paid": 0.0,
                "user_avg_group_size": 1.0,
                "user_favorite_category": "NONE",
                "user_preferred_hour": 12,
                "cat_counts": {},
                "host_counts": {}
            }

        merged = user_res.merge(
            events[["id", "category", "start_at", "created_by_user_id", "price"]],
            left_on="event_id",
            right_on="id",
            how="left"
        )

        cat_counts = Counter(merged["category"].tolist())
        host_counts = Counter(merged["created_by_user_id"].tolist())

        return {
            "user_total_bookings": int(len(user_res)),
            "user_avg_price_paid": round(float(user_res["total_price"].mean()), 2),
            "user_avg_group_size": round(float(user_res["persons_count"].mean()), 2),
            "user_favorite_category": cat_counts.most_common(1)[0][0] if cat_counts else "NONE",
            "user_preferred_hour": int(pd.to_datetime(merged["start_at"]).dt.hour.mode().iloc[0]) if not merged.empty else 12,
            "cat_counts": dict(cat_counts),
            "host_counts": dict(host_counts)
        }

    def _synthetic_user_features(self, profile, matched_events):
        if matched_events:
            avg_price = round(sum(e["price"] for e in matched_events) / len(matched_events), 2)
            fav_cat = Counter(e["category"] for e in matched_events).most_common(1)[0][0]
            host_counts = Counter(e["created_by_user_id"] for e in matched_events)
        else:
            avg_price = float(sum(profile["budget"]) / 2)
            fav_cat = profile["preferred"][0]
            host_counts = {}

        return {
            "user_total_bookings": len(matched_events),
            "user_avg_price_paid": avg_price,
            "user_avg_group_size": profile["avg_group_size"],
            "user_favorite_category": fav_cat,
            "user_preferred_hour": profile["preferred_hour"],
            "cat_counts": Counter(e["category"] for e in matched_events),
            "host_counts": dict(host_counts)
        }

    def _synthetic_label(self, event, profile):
        cat_match = event["category"] in profile["preferred"]
        budget_ok = profile["budget"][0] <= float(event["price"]) <= profile["budget"][1]
        hour = pd.to_datetime(event["start_at"]).hour

        score = 0
        if cat_match:
            score += 2
        if budget_ok:
            score += 1
        if abs(hour - profile["preferred_hour"]) <= 3:
            score += 1
        if float(event.get("event_avg_rating", 0)) >= 4:
            score += 1

        if score >= 4:
            return 3
        if score == 3:
            return 2
        if score == 2:
            return 1
        return 0

    def build(self):
        real_users = self.load_real_tourists()
        events = self.load_events()
        reservations = self.load_reservations()
        event_stats = self.load_event_stats()

        # Use real tourists from DB, then add synthetic demo tourists until we reach 5
        all_users = list(real_users)
        synthetic_users = {}
        for uid, profile in SYNTHETIC_PROFILES.items():
            if len(all_users) >= TARGET_TOURISTS:
                break
            all_users.append(uid)
            synthetic_users[uid] = profile

        rows = []

        # Precompute real user features once
        real_user_features = {}
        for uid in real_users:
            real_user_features[uid] = self._real_user_features(uid, reservations, events)

        # Create a synthetic history per synthetic user
        synthetic_histories = {}
        for uid, profile in synthetic_users.items():
            matching = []
            for _, ev in events.iterrows():
                if ev["category"] in profile["preferred"] and profile["budget"][0] <= float(ev["price"]) <= profile["budget"][1]:
                    matching.append(ev.to_dict())
            random.Random(uid).shuffle(matching)
            synthetic_histories[uid] = matching[:max(8, min(15, len(matching)))]

        # Build rows for every user-event pair
        for uid in all_users:
            is_synthetic = uid in synthetic_users

            if is_synthetic:
                profile = synthetic_users[uid]
                user_hist = synthetic_histories.get(uid, [])
                uf = self._synthetic_user_features(profile, user_hist)
                user_confirmed_event_ids = {e["id"] for e in user_hist}
            else:
                uf = real_user_features.get(uid, {
                    "user_total_bookings": 0,
                    "user_avg_price_paid": 0.0,
                    "user_avg_group_size": 1.0,
                    "user_favorite_category": "NONE",
                    "user_preferred_hour": 12,
                    "cat_counts": {},
                    "host_counts": {}
                })
                user_confirmed_event_ids = set(
                    reservations[
                        (reservations["user_id"] == uid) &
                        (reservations["status"] == "CONFIRMED") &
                        (reservations["payment_status"] == "PAID")
                    ]["event_id"].tolist()
                )

            for _, ev in events.iterrows():
                ev = ev.to_dict()
                stats = event_stats.get(ev["id"], {
                    "event_popularity": 0,
                    "event_avg_rating": 0.0,
                    "event_avg_sentiment": 0.0
                })

                price_ratio = round(ev["price"] / uf["user_avg_price_paid"], 4) if uf["user_avg_price_paid"] > 0 else 1.0
                category_match = int(ev["category"] == uf["user_favorite_category"])
                cat_novelty = int(ev["category"] not in uf["cat_counts"])

                start_dt = pd.to_datetime(ev["start_at"])
                days_until = max((start_dt - datetime.now()).days, 0)
                is_weekend = int(start_dt.weekday() >= 5)
                availability = round(1.0 - (float(ev.get("booked_seats", 0) or 0) / max(int(ev["capacity"]), 1)), 4)
                event_hour = int(start_dt.hour)

                same_category = int(uf["cat_counts"].get(ev["category"], 0))
                same_host = int(uf["host_counts"].get(ev["created_by_user_id"], 0))

                if is_synthetic:
                    label = self._synthetic_label({**ev, **stats}, profile)
                else:
                    r = reservations[
                        (reservations["user_id"] == uid) &
                        (reservations["event_id"] == ev["id"])
                    ]
                    if r.empty:
                        label = 0
                    else:
                        row = r.iloc[0]
                        if row["status"] == "CONFIRMED" and row["payment_status"] == "PAID":
                            label = 3
                        elif row["status"] == "CONFIRMED":
                            label = 2
                        elif row["status"] == "WAITLISTED":
                            label = 1
                        else:
                            label = 0

                rows.append({
                    "user_id": uid,
                    "event_id": ev["id"],
                    "label": label,

                    "user_total_bookings": uf["user_total_bookings"],
                    "user_avg_price_paid": uf["user_avg_price_paid"],
                    "user_avg_group_size": uf["user_avg_group_size"],
                    "user_favorite_category": uf["user_favorite_category"],
                    "user_preferred_hour": uf["user_preferred_hour"],

                    "event_category": ev["category"],
                    "event_price": float(ev["price"]),
                    "event_capacity": int(ev["capacity"]),
                    "event_hour": event_hour,
                    "event_is_weekend": is_weekend,
                    "event_days_until": days_until,
                    "event_availability": availability,
                    "event_popularity": int(stats["event_popularity"]),
                    "event_avg_rating": round(float(stats["event_avg_rating"]), 3),
                    "event_avg_sentiment": round(float(stats["event_avg_sentiment"]), 3),

                    "interaction_same_category": same_category,
                    "interaction_same_host": same_host,

                    "context_price_ratio": price_ratio,
                    "context_category_match": category_match,
                    "context_cat_novelty": cat_novelty,
                    "context_hour_diff": abs(event_hour - uf["user_preferred_hour"]),
                })

        df = pd.DataFrame(rows).sort_values(["user_id", "event_id"]).reset_index(drop=True)
        df.to_csv("ranking_dataset.csv", index=False)

        print("Dataset shape:", df.shape)
        print("Label distribution:", df["label"].value_counts().to_dict())
        print("Users:", df["user_id"].nunique(), "Events:", df["event_id"].nunique())
        print("Saved ranking_dataset.csv")
        return df

if __name__ == "__main__":
    DatasetBuilder().build()