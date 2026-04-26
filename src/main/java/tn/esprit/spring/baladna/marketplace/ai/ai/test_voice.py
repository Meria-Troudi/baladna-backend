import edge_tts
import asyncio

async def main():
    tts = edge_tts.Communicate(
        'Discover our beautiful pottery! Only 45 TND. Order now!',
        'en-US-JennyNeural'
    )
    await tts.save('videos/test_voice.mp3')
    print('✅ Voice saved!')

asyncio.run(main())