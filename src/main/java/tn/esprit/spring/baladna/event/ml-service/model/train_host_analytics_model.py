import os
import json
import joblib
import numpy as np
import pandas as pd

from lightgbm import LGBMRegressor
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import mean_squared_error, r2_score

# ================= LOAD =================
df = pd.read_csv("dataset_host_analytics.csv")

# ⚠️ REQUIRED: dataset must include start_at or timestamp column
# If not → you MUST add it in builder
if "start_at" not in df.columns:
    raise ValueError("Dataset must include 'start_at' for time-based split")

df["start_at"] = pd.to_datetime(df["start_at"])

# ================= SORT (CRITICAL) =================
df = df.sort_values("start_at")

# ================= FEATURE / TARGET SPLIT =================
TARGETS = [
    "fill_rate",
    "expected_revenue",
    "avg_rating"
]

DROP_COLS = [
    "event_id",
    "host_id",
    "start_at",
    "total_reservations",
    "confirmed_seats",
    "avg_sentiment",
    "review_count"
]

X = df.drop(columns=TARGETS + DROP_COLS)

y_fill = df["fill_rate"]
y_rev = df["expected_revenue"]
y_rating = df["avg_rating"]

# ================= ENCODING =================
le = LabelEncoder()
X["category"] = le.fit_transform(X["category"].astype(str))

# ================= TIME SPLIT (NO LEAKAGE) =================
# 80% past → train, 20% future → test

split_index = int(len(df) * 0.8)

X_train = X.iloc[:split_index]
X_test = X.iloc[split_index:]

y_fill_train = y_fill.iloc[:split_index]
y_fill_test = y_fill.iloc[split_index:]

y_rev_train = y_rev.iloc[:split_index]
y_rev_test = y_rev.iloc[split_index:]

y_rating_train = y_rating.iloc[:split_index]
y_rating_test = y_rating.iloc[split_index:]

print(f"Train size: {X_train.shape}, Test size: {X_test.shape}")

# ================= MODELS =================
def train_model(name, y_tr, y_te):
    model = LGBMRegressor(
        n_estimators=200,
        learning_rate=0.05,
        max_depth=6,
        subsample=0.8,
        colsample_bytree=0.8,
        random_state=42
    )

    model.fit(X_train, y_tr)

    preds = model.predict(X_test)

    rmse = np.sqrt(mean_squared_error(y_te, preds))
    r2 = r2_score(y_te, preds)

    print(f"\n{name} Model:")
    print(f"RMSE: {rmse:.4f}")
    print(f"R²:   {r2:.4f}")

    return model

model_fill = train_model("Fill Rate", y_fill_train, y_fill_test)
model_rev = train_model("Revenue", y_rev_train, y_rev_test)
model_rating = train_model("Rating", y_rating_train, y_rating_test)

# ================= SAVE =================
os.makedirs("model", exist_ok=True)

joblib.dump(model_fill, "model/host_fill_rate_model.pkl")
joblib.dump(model_rev, "model/host_revenue_model.pkl")
joblib.dump(model_rating, "model/host_rating_model.pkl")

joblib.dump(le, "model/host_category_encoder.pkl")

with open("model/host_feature_names.json", "w") as f:
    json.dump(list(X.columns), f)

print("\n✅ Models saved successfully")