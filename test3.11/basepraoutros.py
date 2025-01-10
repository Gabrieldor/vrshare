import socket
import asyncio
import io
from PIL import ImageGrab, Image
from concurrent.futures import ThreadPoolExecutor

connected = False

def retrieve_screenshot():
    loop = range(2147483647)
    for _ in loop:
        try:
            img = ImageGrab.grab()
            output = io.BytesIO()
            img = img.resize((1170, 1080), Image.Resampling.BOX)
            img.save(output, 'jpeg', quality=30)
            img_data = output.getvalue()
            size = len(img_data)
            yield size.to_bytes(4, 'big') + img_data
        except Exception as e:
            print(f"Error: {e}")
            break

async def handle_client(reader, writer):
    global connected
    if not connected:
        print('Client connected IP:', writer.get_extra_info('peername'))
        connected = True

    async for data in retrieve_screenshot():
        writer.write(data)
        await writer.drain()

def main(port=5000):
    global connected
    connected = False
    loop = asyncio.get_event_loop()

    async def start_server():
        server = await asyncio.start_server(handle_client, '192.168.0.20', port)
        print(f'Server started on {server.sockets[0].getsockname()}')
        await server.serve_forever()

    with ThreadPoolExecutor() as executor:
        loop.run_until_complete(asyncio.gather(
            loop.run_in_executor(executor, ImageGrab.grab),
            start_server()
        ))

if __name__ == '__main__':
    main()
