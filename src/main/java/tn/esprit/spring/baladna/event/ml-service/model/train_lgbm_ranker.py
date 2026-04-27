import pandas as pd
import joblib
import json
from lightgbm import LGBMRanker
from sklearn.preprocessing import LabelEncoder

# Load dataset
import os
script_dir = os.path.dirname(os.path.abspath(__file__))
csv_path = os.path.join(script_dir, "ranking_dataset.csv")
df = pd.read_csv(csv_path)

y = df["label"]
groups = df.groupby("user_id").size().values

X = df.drop(columns=["label", "user_id", "event_id"])

# Encode categoricals
encoders = {}
for col in ["user_favorite_category", "event_category"]:
    le = LabelEncoder()
    X[col] = le.fit_transform(X[col].astype(str))
    encoders[col] = le

# Train LightGBM Ranker
model = LGBMRanker(
    objective="lambdarank",
    n_estimators=100
)

model.fit(X, y, group=groups)

# Save artifacts
model_dir = os.path.join(script_dir, "model")
os.makedirs(model_dir, exist_ok=True)

joblib.dump(model, os.path.join(model_dir, "model.pkl"))
joblib.dump(encoders, os.path.join(model_dir, "encoders.pkl"))

# Save label encoder classes for categorical features
label_encoder_classes = {col: list(encoders[col].classes_) for col in encoders}
with open(os.path.join(model_dir, "label_encoder_classes.json"), "w") as f:
    json.dump(label_encoder_classes, f, indent=2)

with open(os.path.join(model_dir, "feature_names.json"), "w") as f:
    json.dump(list(X.columns), f)

print("Model trained")
print("Features:", list(X.columns))
