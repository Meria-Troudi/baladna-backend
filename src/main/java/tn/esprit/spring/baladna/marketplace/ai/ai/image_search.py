from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import torch
import numpy as np
from transformers import CLIPProcessor, CLIPModel
from sklearn.metrics.pairwise import cosine_similarity
import io
import base64
import requests
import json

app = FastAPI(title="Baladna Visual Search")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load CLIP model once
print("⏳ Loading CLIP model...")
model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")
print("✅ CLIP model ready!")

# Product database (will be populated by backend)
product_features = {}
product_metadata = {}


def extract_image_features(image: Image.Image) -> np.ndarray:
    """Extract CLIP features from an image."""
    inputs = processor(images=image, return_tensors="pt")
    with torch.no_grad():
        features = model.get_image_features(**inputs)
    return features.numpy().flatten()


def compute_similarity(query_features: np.ndarray, product_features_list: list) -> list:
    """Compute cosine similarity between query and all products."""
    if not product_features_list:
        return []

    all_features = np.array([f for f in product_features_list])
    similarities = cosine_similarity([query_features], all_features)[0]

    # Get top 5 matches
    top_indices = np.argsort(similarities)[::-1][:5]

    results = []
    for idx in top_indices:
        if similarities[idx] > 0.15:  # Seuil minimum de similarité
            results.append({
                "index": int(idx),
                "score": float(similarities[idx]),
                "productId": list(product_metadata.keys())[idx] if idx < len(product_metadata) else None
            })

    return results


@app.post("/search")
async def search_similar_products(file: UploadFile = File(...)):
    """Search for products similar to the uploaded image."""
    try:
        # Read and process the uploaded image
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")

        # Extract features
        query_features = extract_image_features(image)

        # Get product features from backend
        try:
            response = requests.get("http://localhost:8081/api/visual-search/product-features", timeout=5)
            if response.status_code == 200:
                data = response.json()
                global product_features, product_metadata
                product_features = {int(k): np.array(v) for k, v in data.get("features", {}).items()}
                product_metadata = data.get("metadata", {})
                print(f"📦 Loaded {len(product_features)} products")
        except Exception as e:
            print(f"⚠️ Could not load product features: {e}")

        # Get all features as list
        features_list = [product_features[k] for k in sorted(product_features.keys())]

        # Compute similarities
        results = compute_similarity(query_features, features_list)

        # Build response with product details
        products = []
        for r in results:
            product_id = r["productId"]
            if product_id and str(product_id) in product_metadata:
                meta = product_metadata[str(product_id)]
                products.append({
                    "id": product_id,
                    "name": meta.get("name", "Unknown"),
                    "price": meta.get("price", 0),
                    "image": meta.get("image", ""),
                    "category": meta.get("category", ""),
                    "score": round(r["score"] * 100, 1)
                })

        return {
            "success": True,
            "products": products,
            "count": len(products)
        }

    except Exception as e:
        print(f"❌ Error: {e}")
        return {"success": False, "error": str(e), "products": []}


@app.post("/index-product")
async def index_product(productId: int, imageUrl: str, productName: str, price: float, category: str):
    """Index a single product for visual search."""
    try:
        response = requests.get(imageUrl, timeout=10)
        image = Image.open(io.BytesIO(response.content)).convert("RGB")
        features = extract_image_features(image)

        product_features[productId] = features.tolist()
        product_metadata[str(productId)] = {
            "name": productName,
            "price": price,
            "image": imageUrl,
            "category": category
        }

        return {"success": True, "productId": productId}
    except Exception as e:
        return {"success": False, "error": str(e)}


@app.get("/health")
async def health():
    return {"status": "UP", "products_indexed": len(product_features)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)