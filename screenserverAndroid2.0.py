import socket
from threading import Thread
import ctypes
import io
from PIL import ImageGrab
from PIL import Image
import time

user32 = ctypes.windll.user32
WIDTH = user32.GetSystemMetrics(0)
HEIGHT = user32.GetSystemMetrics(1)


def retreive_screenshot(conn):
        while True:
            start_t = time.perf_counter()
            output = io.BytesIO()
            img = ImageGrab.grab()
            img = img.resize((1024,768),Image.ANTIALIAS)
            img.save(output,'jpeg',quality=75)
            img_png = output.getvalue()
            size = len(img_png)
            size_division = (size // 256)
            conn.send(size.to_bytes(4,'big') + img_png)
            end_t = time.perf_counter()
            wall_duration = end_t - start_t
            print(f"it took: {wall_duration:.2f}s")

def main(host='192.168.0.20', port=5000):
    sock = socket.socket()      
    sock.bind((host, port)) 
    try:
        sock.listen(2)
        print('Server started.')

        while 'connected':
            conn, addr = sock.accept()
            print('Client connected IP:', addr)
            retreive_screenshot(conn)
    finally:
        sock.close()


if __name__ == '__main__':
    main()