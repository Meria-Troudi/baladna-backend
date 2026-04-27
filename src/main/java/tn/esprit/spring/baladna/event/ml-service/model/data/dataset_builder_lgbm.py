import random
from collections import Counter
from datetime import datetime
import pandas as pd
from sqlalchemy import create_engine
import numpy as np

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "baladna"
}
TARGET_TOURISTS = 5
SYNTHETIC_PROFILES = {
    1001: {"name": "DemoA", "preferred": ["MUSIC", "NIGHTLIFE", "FESTIVAL"],
           "budget": (30, 90), "preferred_hour": 21, "avg_group_size": 2.0, "noise": 0.25},
    1002: {"name": "DemoB", "preferred": ["SPORT", "OUTDOOR", "TOUR"],
           "budget": (15, 70), "preferred_hour": 10, "avg_group_size": 3.0, "noise": 0.25}
}

class DatasetBuilder:
    def __init__(self):
        self.engine = create_engine(f"mysql+mysqlconnector://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}/{DB_CONFIG['database']}")

    def load_real_tourists(self):
        df = pd.read_sql("SELECT id FROM user WHERE role = 'TOURIST' ORDER BY id", self.engine)
        return df["id"].tolist()

    def load_events(self):
        return pd.read_sql("""
            SELECT e.id, e.title, e.category, e.price, e.capacity, e.booked_seats,
                   e.start_at, e.end_at, e.latitude, e.longitude, e.created_by_user_id
            FROM event e WHERE e.status = 'UPCOMING' ORDER BY e.id
        """, self.engine)

    def load_reservations(self):
        return pd.read_sql("""
            SELECT er.id, er.user_id, er.event_id, er.persons_count, er.total_price,
                   er.status, er.payment_status, er.created_at
            FROM event_reservation er ORDER BY er.id
        """, self.engine)

    def load_event_stats(self):
        df = pd.read_sql("""
            SELECT e.id AS event_id,
                   COALESCE(COUNT(DISTINCT er.id), 0) AS event_popularity,
                   COALESCE(AVG(rv.rating), 0) AS event_avg_rating,
                   COALESCE(AVG(rv.sentiment_score), 0) AS event_avg_sentiment
            FROM event e
            LEFT JOIN event_reservation er ON er.event_id = e.id AND er.status <> 'CANCELLED'
            LEFT JOIN event_review rv ON rv.event_id = e.id
            GROUP BY e.id
        """, self.engine)
        return df.set_index("event_id").to_dict(orient="index")

    def _real_user_features(self, user_id, reservations, events):
        user_res = reservations[(reservations["user_id"] == user_id) &
                                (reservations["status"] == "CONFIRMED") &
                                (reservations["payment_status"] == "PAID")]
        if user_res.empty:
            return {"user_total_bookings": 0, "user_avg_price_paid": 0.0, "user_avg_group_size": 1.0,
                    "user_favorite_category": "NONE", "user_preferred_hour": 12, "cat_counts": {},
                    "host_counts": {}, "user_price_min": 0, "user_price_max": 0, "user_price_std": 0.0}
        merged = user_res.merge(events[["id", "category", "start_at", "created_by_user_id", "price"]],
                                left_on="event_id", right_on="id", how="left")
        cat_counts = Counter(merged["category"])
        host_counts = Counter(merged["created_by_user_id"])
        prices = merged["price"].tolist()
        if prices:
            user_price_min, user_price_max = min(prices), max(prices)
            user_price_std = np.std(prices) if len(prices) > 1 else 0.0
        else:
            user_price_min = user_price_max = user_price_std = 0.0
        return {
            "user_total_bookings": len(user_res),
            "user_avg_price_paid": round(user_res["total_price"].mean(), 2),
            "user_avg_group_size": round(user_res["persons_count"].mean(), 2),
            "user_favorite_category": cat_counts.most_common(1)[0][0] if cat_counts else "NONE",
            "user_preferred_hour": int(pd.to_datetime(merged["start_at"]).dt.hour.mode()[0]) if not merged.empty else 12,
            "cat_counts": dict(cat_counts), "host_counts": dict(host_counts),
            "user_price_min": user_price_min, "user_price_max": user_price_max, "user_price_std": user_price_std
        }

    def _synthetic_user_features(self, profile, matched_events):
        if matched_events:
            avg_price = round(sum(e["price"] for e in matched_events) / len(matched_events), 2)
            fav_cat = Counter(e["category"] for e in matched_events).most_common(1)[0][0]
            host_counts = Counter(e["created_by_user_id"] for e in matched_events)
            prices = [e["price"] for e in matched_events]
            user_price_min, user_price_max = min(prices), max(prices)
            user_price_std = np.std(prices) if len(prices) > 1 else 0.0
        else:
            fav_cat = profile["preferred"][0]
            avg_price = sum(profile["budget"]) / 2
            host_counts = {}
            user_price_min = user_price_max = user_price_std = 0.0
        return {
            "user_total_bookings": len(matched_events), "user_avg_price_paid": avg_price,
            "user_avg_group_size": profile["avg_group_size"], "user_favorite_category": fav_cat,
            "user_preferred_hour": profile["preferred_hour"], "cat_counts": Counter(e["category"] for e in matched_events),
            "host_counts": dict(host_counts), "user_price_min": user_price_min, "user_price_max": user_price_max,
            "user_price_std": user_price_std
        }

    def _synthetic_label(self, event, profile):
        cat_match = event["category"] in profile["preferred"]
        budget_ok = profile["budget"][0] <= event["price"] <= profile["budget"][1]
        hour = pd.to_datetime(event["start_at"]).hour
        score = 0
        if cat_match:
            score += 2
        if budget_ok:
            score += 1
        if abs(hour - profile["preferred_hour"]) <= 3:
            score += 1
        if event.get("event_avg_rating", 0) >= 4:
            score += 1
        
        # Add random noise
        if random.random() < profile.get("noise", 0.25):
            flip = random.choice([-1, 0, 1])
            score = max(0, min(5, score + flip))

        # SAME EXACT LABEL SCALE AS REAL USERS
        if score >= 4:
            return 3   # PAID - same as real
        elif score == 3:
            return 2   # CONFIRMED - same as real
        elif score == 2:
            return 1   # WAITLISTED - same as real
        else:
            return 0   # NO INTERACTION - same as real

    def build(self):
        real_users = self.load_real_tourists()
        events = self.load_events()
        reservations = self.load_reservations()
        event_stats = self.load_event_stats()

        all_users = list(real_users)
        synthetic_users = {}
        for uid, profile in SYNTHETIC_PROFILES.items():
            if len(all_users) >= TARGET_TOURISTS:
                break
            all_users.append(uid)
            synthetic_users[uid] = profile

        real_user_features = {uid: self._real_user_features(uid, reservations, events) for uid in real_users}
        synthetic_histories = {}
        for uid, profile in synthetic_users.items():
            matching = [ev.to_dict() for _, ev in events.iterrows()
                        if ev["category"] in profile["preferred"] and profile["budget"][0] <= ev["price"] <= profile["budget"][1]]
            random.Random(uid).shuffle(matching)
            synthetic_histories[uid] = matching[:max(8, min(15, len(matching)))]

        rows = []
        for uid in all_users:
            is_synthetic = uid in synthetic_users
            if is_synthetic:
                profile = synthetic_users[uid]
                user_hist = synthetic_histories.get(uid, [])
                uf = self._synthetic_user_features(profile, user_hist)
                user_confirmed_event_ids = {e["id"] for e in user_hist}
            else:
                uf = real_user_features.get(uid, {"user_total_bookings": 0, "user_avg_price_paid": 0.0, "user_avg_group_size": 1.0,
                                                  "user_favorite_category": "NONE", "user_preferred_hour": 12,
                                                  "cat_counts": {}, "host_counts": {}, "user_price_min": 0, "user_price_max": 0,
                                                  "user_price_std": 0.0})
                user_confirmed_event_ids = set(reservations[(reservations["user_id"] == uid) &
                                                            (reservations["status"] == "CONFIRMED") &
                                                            (reservations["payment_status"] == "PAID")]["event_id"])

            for _, ev in events.iterrows():
                ev_dict = ev.to_dict()
                stats = event_stats.get(ev_dict["id"], {"event_popularity": 0, "event_avg_rating": 0.0, "event_avg_sentiment": 0.0})
                start_dt = pd.to_datetime(ev_dict["start_at"])
                event_hour = start_dt.hour
                price_ratio = ev_dict["price"] / uf["user_avg_price_paid"] if uf["user_avg_price_paid"] > 0 else 1.0
                category_match = int(ev_dict["category"] == uf["user_favorite_category"])
                cat_novelty = int(ev_dict["category"] not in uf["cat_counts"])
                same_category = int(uf["cat_counts"].get(ev_dict["category"], 0))
                same_host = int(uf["host_counts"].get(ev_dict["created_by_user_id"], 0))
                price_within_range = int(uf["user_price_min"] <= ev_dict["price"] <= uf["user_price_max"])
                price_within_std = int(uf["user_price_std"] > 0 and (uf["user_price_min"] + uf["user_price_std"] >= ev_dict["price"] >= uf["user_price_min"] - uf["user_price_std"]))

                if is_synthetic:
                    label = self._synthetic_label({**ev_dict, **stats}, profile)
                else:
                    r = reservations[(reservations["user_id"] == uid) & (reservations["event_id"] == ev_dict["id"])]
                    if r.empty:
                        label = 0
                    else:
                        row = r.iloc[0]
                        label = 3 if row["status"] == "CONFIRMED" and row["payment_status"] == "PAID" else 2 if row["status"] == "CONFIRMED" else 1 if row["status"] == "WAITLISTED" else 0

                rows.append({
                    "user_id": uid, "event_id": ev_dict["id"], "label": label,
                    "user_total_bookings": uf["user_total_bookings"], "user_avg_price_paid": uf["user_avg_price_paid"],
                    "user_avg_group_size": uf["user_avg_group_size"], "user_favorite_category": uf["user_favorite_category"],
                    "user_preferred_hour": uf["user_preferred_hour"],
                    "event_category": ev_dict["category"], "event_price": float(ev_dict["price"]),
                    "event_capacity": int(ev_dict["capacity"]), "event_hour": event_hour,
                    "event_is_weekend": int(start_dt.weekday() >= 5),
                    "event_days_until": max((start_dt - datetime.now()).days, 0),
                    "event_availability": round(1.0 - (float(ev_dict.get("booked_seats",0) or 0)/max(int(ev_dict["capacity"]),1))
                                                
                                                
                                                , 4),
                    "event_popularity": int(stats["event_popularity"]),
                    "event_avg_rating": round(stats["event_avg_rating"], 3),
                    "event_avg_sentiment": round(stats["event_avg_sentiment"], 3),
                    "interaction_same_category": same_category, "interaction_same_host": same_host,
                    "context_price_ratio": round(price_ratio, 4), "context_category_match": category_match,
                    "context_cat_novelty": cat_novelty, "context_hour_diff": abs(event_hour - uf["user_preferred_hour"]),
                    "context_price_within_range": price_within_range, "context_price_within_std": price_within_std
                })
        import os
        script_dir = os.path.dirname(os.path.abspath(__file__))
        output_path = os.path.abspath(os.path.join(script_dir, "..", "ranking_dataset.csv"))
        df = pd.DataFrame(rows).sort_values(["user_id","event_id"]).reset_index(drop=True)
        df.to_csv(output_path, index=False)
        print(f"Dataset shape: {df.shape}, Label dist: {df['label'].value_counts().to_dict()}")
        return df

if __name__ == "__main__":
    DatasetBuilder().build()