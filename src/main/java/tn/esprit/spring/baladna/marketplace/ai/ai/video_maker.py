from fastapi import FastAPI, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
import os
import uuid
import subprocess
import asyncio
import edge_tts
import random
import time

app = FastAPI(title="Baladna Video Maker")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

VIDEO_DIR = "videos"
FONT_PATH = "/Windows/Fonts/arial.ttf"
FONT_BOLD_PATH = "/Windows/Fonts/arialbd.ttf"
os.makedirs(VIDEO_DIR, exist_ok=True)


def generate_script(product_name: str, price: float, category: str) -> str:
    templates = [
        f"Discover our beautiful {product_name}! Handcrafted by Tunisian artisans. Only {price:.0f} TND. Order now on Baladna!",
        f"Looking for authentic {category}? This {product_name} is handmade with traditional techniques. Just {price:.0f} TND. Visit Baladna today!",
        f"Unique {product_name} - crafted by skilled artisans in Tunisia. Premium quality at {price:.0f} TND. Get yours on Baladna!",
    ]
    return random.choice(templates)


async def generate_voice(text: str, output_path: str):
    tts = edge_tts.Communicate(text, "en-US-JennyNeural")
    await tts.save(output_path)
    return output_path


def create_professional_video(image_path: str, audio_path: str, product_name: str, price: float) -> str:
    output_path = os.path.join(VIDEO_DIR, f"reel_{uuid.uuid4().hex[:8]}.mp4")
    ffmpeg = r"C:\ProgramData\chocolatey\bin\ffmpeg.exe"

    safe_name = product_name.replace("'", "'\\''").replace(":", "\\:")

    # === ANIMATED PRICE TAG (bottom-right popup) ===
    price_tag = (
        f"fontfile={FONT_BOLD_PATH}:text='{price:.0f} TND':fontsize=42:fontcolor=white:"
        f"x=w-tw-30:y=h-th-30:shadowcolor=black:shadowx=3:shadowy=3:"
        f"bordercolor=#FF416C:borderw=5:box=1:boxcolor=#FF416C@0.85:boxborderw=10"
    )

    # === PRODUCT NAME (top-left with background) ===
    name_tag = (
        f"fontfile={FONT_BOLD_PATH}:text='{safe_name}':fontsize=28:fontcolor=white:"
        f"x=20:y=20:shadowcolor=black:shadowx=2:shadowy=2:"
        f"box=1:boxcolor=black@0.55:boxborderw=12"
    )

    # === CATEGORY BADGE (top-right) ===
    badge = (
        f"fontfile={FONT_PATH}:text='NEW':fontsize=18:fontcolor=white:"
        f"x=w-tw-25:y=80:shadowcolor=black:shadowx=1:shadowy=1:"
        f"box=1:boxcolor=#0EA5E9@0.9:boxborderw=8"
    )

    # === CTA BOTTOM (animated banner) ===
    cta = (
        f"fontfile={FONT_BOLD_PATH}:text='🛒 ORDER NOW ON BALADNA':fontsize=22:fontcolor=white:"
        f"x=(w-tw)/2:y=h-55:shadowcolor=black:shadowx=2:shadowy=2:"
        f"box=1:boxcolor=#0EA5E9@0.8:boxborderw=8"
    )

    # === ZOOM + PAN EFFECT (Ken Burns) ===
    zoom_filter = (
        f"[0:v]scale=8000:-1,"
        f"zoompan=z='min(zoom+0.0012,1.3)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
        f"d=150:s=720x500,fps=25,"
        f"drawtext={name_tag},drawtext={badge},drawtext={price_tag},drawtext={cta}[v]"
    )

    cmd = [
        ffmpeg, '-y',
        '-loop', '1', '-i', image_path,
        '-i', audio_path,
        '-filter_complex', zoom_filter,
        '-map', '[v]', '-map', '1:a',
        '-c:v', 'libx264', '-preset', 'fast', '-crf', '23',
        '-c:a', 'aac', '-b:a', '128k',
        '-pix_fmt', 'yuv420p', '-shortest',
        '-movflags', '+faststart',
        output_path
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        print(f"FFmpeg STDERR: {result.stderr}")
        raise Exception(f"FFmpeg error: {result.stderr[:200]}")

    return output_path


@app.post("/generate-video")
async def generate_video(
    file: UploadFile = File(...),
    productName: str = Form(...),
    price: float = Form(...),
    category: str = Form("handicraft"),
    description: str = Form("")
):
    try:
        img_path = os.path.join(VIDEO_DIR, f"img_{uuid.uuid4().hex[:8]}.jpg")
        with open(img_path, "wb") as f:
            f.write(await file.read())
        time.sleep(0.3)

        script = generate_script(productName, price, category)
        print(f"📝 {script}")

        audio_path = os.path.join(VIDEO_DIR, f"voice_{uuid.uuid4().hex[:8]}.mp3")
        await generate_voice(script, audio_path)

        video_path = create_professional_video(img_path, audio_path, productName, price)
        print(f"🎬 Video: {video_path}")

        os.remove(img_path)
        os.remove(audio_path)

        return FileResponse(video_path, media_type="video/mp4",
                          filename=f"{productName.replace(' ', '_')}_reel.mp4")
    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"error": str(e), "success": False}


@app.get("/health")
async def health():
    return {"status": "UP", "service": "Baladna Reel Maker"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)