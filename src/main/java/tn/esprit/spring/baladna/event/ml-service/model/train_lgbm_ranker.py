import numpy as np
import pandas as pd
import joblib
import json
import optuna
from lightgbm import LGBMRanker
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.model_selection import GroupShuffleSplit
from sklearn.metrics import ndcg_score
import os

def objective(trial, X_train, y_train, groups_train, X_val, y_val, groups_val):
    params = {
        'objective': 'lambdarank',
        'n_estimators': trial.suggest_int('n_estimators', 50, 200),
        'learning_rate': trial.suggest_float('learning_rate', 0.01, 0.1, log=True),
        'num_leaves': trial.suggest_int('num_leaves', 10, 100),
        'min_child_samples': trial.suggest_int('min_child_samples', 5, 50),
        'reg_alpha': trial.suggest_float('reg_alpha', 1e-3, 10.0, log=True),
        'reg_lambda': trial.suggest_float('reg_lambda', 1e-3, 10.0, log=True),
    }
    model = LGBMRanker(**params)
    model.fit(X_train, y_train, group=groups_train, eval_set=[(X_val, y_val)], eval_group=[groups_val])
    
    # Real NDCG@10 calculation
    scores = model.predict(X_val)
    ndcg_scores = []
    start = 0
    for g in groups_val:
        true = y_val[start:start+g]
        pred = scores[start:start+g]
        if len(true) > 1:
            ndcg_scores.append(ndcg_score([true], [pred], k=10))
        start += g
    
    return np.mean(ndcg_scores) if ndcg_scores else 0.0

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.abspath(os.path.join(script_dir, "ranking_dataset.csv"))
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

    # Feature Normalisation
    numeric_cols = ["event_price", "event_popularity", "context_price_ratio", "context_hour_diff"]
    scaler = StandardScaler()
    X[numeric_cols] = scaler.fit_transform(X[numeric_cols])

    # Train/validation split preserving users
    gss = GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=42)
    train_idx, val_idx = next(gss.split(X, y, groups=df['user_id']))
    X_train, X_val = X.iloc[train_idx], X.iloc[val_idx]
    y_train, y_val = y.iloc[train_idx], y.iloc[val_idx]
    user_groups = df.groupby("user_id").size().values
    groups_train = user_groups[np.unique(df.iloc[train_idx]["user_id"].factorize()[0])]
    groups_val = user_groups[np.unique(df.iloc[val_idx]["user_id"].factorize()[0])]

    # Hyperparameter optimisation
    study = optuna.create_study(direction='maximize', pruner=optuna.pruners.MedianPruner())
    study.optimize(lambda trial: objective(trial, X_train, y_train, groups_train, X_val, y_val, groups_val), n_trials=20)

    # Train final model with best params
    best_params = study.best_params
    model = LGBMRanker(objective='lambdarank', **best_params)
    model.fit(X, y, group=groups)

    # Save artifacts
    model_dir = os.path.join(script_dir, "model")
    os.makedirs(model_dir, exist_ok=True)
    joblib.dump(model, os.path.join(model_dir, "model.pkl"))
    joblib.dump(encoders, os.path.join(model_dir, "encoders.pkl"))
    joblib.dump(scaler, os.path.join(model_dir, "scaler.pkl"))
    label_encoder_classes = {col: list(encoders[col].classes_) for col in encoders}
    with open(os.path.join(model_dir, "label_encoder_classes.json"), "w") as f:
        json.dump(label_encoder_classes, f, indent=2)
    with open(os.path.join(model_dir, "feature_names.json"), "w") as f:
        json.dump(list(X.columns), f)
    print("Model trained, best NDCG@10:", study.best_value)

if __name__ == "__main__":
    main()