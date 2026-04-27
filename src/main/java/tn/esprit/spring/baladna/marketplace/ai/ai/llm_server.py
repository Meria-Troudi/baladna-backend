from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForCausalLM, pipeline
import torch
import json
import re

app = FastAPI(title="Baladna LLM Server")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =============================================
# MODÈLE : Utilise Mistral-7B (gratuit, puissant)
# =============================================
model_name = "mistralai/Mistral-7B-Instruct-v0.2"

print("⏳ Loading model...")
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16,
    device_map="auto",
    low_cpu_mem_usage=True
)
pipe = pipeline("text-generation", model=model, tokenizer=tokenizer)
print("✅ Model loaded!")

# =============================================
# PROMPT SYSTÈME BALADNA
# =============================================
SYSTEM_PROMPT = """<s>[INST] You are the AI assistant for Baladna, a Tunisian artisan marketplace.
You help artisans manage their products, orders, and negotiate prices with customers.

CAPABILITIES:
- Add, delete, update products via voice or text
- Search Unsplash for product images
- Provide statistics and analytics
- Negotiate prices with customers
- Answer questions in French, English, or Arabic

RULES:
1. If the user wants to perform an ACTION (add/delete/update product, get stats, get orders) → Return ONLY this JSON:
   - Add product: {"action":"ADD_PRODUCT","productName":"NAME","price":PRICE,"stock":STOCK,"category":"CATEGORY"}
   - Delete product: {"action":"DELETE_PRODUCT","productId":ID}
   - Update price: {"action":"UPDATE_PRICE","productId":ID,"price":NEW_PRICE}
   - Get products: {"action":"GET_PRODUCTS"}
   - Get orders: {"action":"GET_ORDERS"}
   - Get stats: {"action":"GET_STATS"}

2. For price NEGOTIATION → Return:
   {"action":"NEGOTIATE","originalPrice":X,"suggestedPrice":Y,"message":"negotiation message"}

3. For CONVERSATION → Answer naturally in the user's language.
   Be friendly, helpful, and knowledgeable about Tunisian handicrafts.

EXAMPLES:
User: "add a Berber carpet at 350 TND with 5 in stock"
You: {"action":"ADD_PRODUCT","productName":"Berber carpet","price":350.0,"stock":5,"category":"carpet"}

User: "show my products"
You: {"action":"GET_PRODUCTS"}

User: "hello how are you?"
You: Hello! I'm doing great, thank you. I'm here to help you manage your artisan boutique on Baladna. What would you like to do today?

User: "can you give me a discount on this carpet?"
You: {"action":"NEGOTIATE","originalPrice":350,"suggestedPrice":315,"message":"I can offer you a 10% discount, so 315 TND instead of 350. This is a handcrafted Berber carpet made with natural wool."}

Now respond to the user's message below.
[/INST]"""

# =============================================
# MODÈLE DE REQUÊTE
# =============================================
class ChatRequest(BaseModel):
    message: str
    artisanId: int = 1
    artisanName: str = "Artisan"

class NegotiateRequest(BaseModel):
    productId: int
    productName: str
    originalPrice: float
    customerOffer: float

# =============================================
# ENDPOINT CHAT
# =============================================
@app.post("/chat")
async def chat(request: ChatRequest):
    try:
        prompt = SYSTEM_PROMPT + f"\n\nUser: {request.message}\nAssistant:"

        response = pipe(
            prompt,
            max_new_tokens=200,
            temperature=0.3,
            do_sample=True,
            top_p=0.9,
        )

        generated_text = response[0]['generated_text']

        # Extraire la réponse après le prompt
        if "Assistant:" in generated_text:
            ai_response = generated_text.split("Assistant:")[-1].strip()
        else:
            ai_response = generated_text

        # Si c'est un JSON, extraire proprement
        json_match = re.search(r'\{.*\}', ai_response, re.DOTALL)
        if json_match:
            json_str = json_match.group()
            # Nettoyer les virgules dans les nombres
            json_str = re.sub(r'(\d),(\d)', r'\1.\2', json_str)
            try:
                action_data = json.loads(json_str)
                if "action" in action_data:
                    return {
                        "success": True,
                        "type": "ACTION",
                        "aiRaw": json_str,
                        **action_data
                    }
            except:
                pass

        return {
            "success": True,
            "type": "TEXT_RESPONSE",
            "message": ai_response,
            "aiRaw": ai_response
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# =============================================
# ENDPOINT NÉGOCIATION
# =============================================
@app.post("/negotiate")
async def negotiate(request: NegotiateRequest):
    prompt = f"""<s>[INST]
You are negotiating a price for a product on Baladna marketplace.

Product: {request.productName}
Original price: {request.originalPrice} TND
Customer offer: {request.customerOffer} TND

As a seller, negotiate wisely:
- Don't go below 60% of original price
- Highlight the craftsmanship and quality
- Suggest a fair counter-offer
- Be friendly but professional

Return ONLY this JSON:
{{"action":"NEGOTIATE_RESPONSE","finalPrice":PRICE,"accepted":true/false,"message":"your negotiation message"}}
[/INST]"""

    response = pipe(prompt, max_new_tokens=150, temperature=0.5)
    ai_response = response[0]['generated_text'].split("[/INST]")[-1].strip()

    json_match = re.search(r'\{.*\}', ai_response, re.DOTALL)
    if json_match:
        try:
            return json.loads(json_match.group())
        except:
            pass

    # Fallback : logique simple de négociation
    min_price = request.originalPrice * 0.6
    counter = max(request.customerOffer * 1.1, min_price)
    accepted = request.customerOffer >= min_price

    return {
        "action": "NEGOTIATE_RESPONSE",
        "finalPrice": round(counter, 2),
        "accepted": accepted,
        "message": f"I can offer {round(counter, 2)} TND. This is a handcrafted piece made by skilled Tunisian artisans."
    }

# =============================================
# HEALTH CHECK
# =============================================
@app.get("/health")
async def health():
    return {"status": "UP", "service": "Baladna LLM", "model": model_name}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)