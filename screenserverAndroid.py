import socket
from threading import Thread
import ctypes
import mss
import mss.tools


user32 = ctypes.windll.user32
WIDTH = user32.GetSystemMetrics(0)
HEIGHT = user32.GetSystemMetrics(1)


def retreive_screenshot(conn):
    with mss.mss() as sct:
        # The region to capture
        rect = {'top': 0, 'left': 0, 'width': 1920, 'height':1080 }

        while 'recording':
            # Capture the screen
            img = sct.grab(rect)
            img_png = mss.tools.to_png(img.rgb,img.size,level=3)
            size = len(img_png)
            size_division = (size // 256)
            conn.send(size_division.to_bytes(2,'big') + size.to_bytes(4,'big') + img_png)

def main(host='192.168.0.20', port=5000):
    sock = socket.socket()      
    sock.bind((host, port)) 
    try:
        sock.listen(2)
        print('Server started.')

        while 'connected':
            conn, addr = sock.accept()
            print('Client connected IP:', addr)
            thread = Thread(target=retreive_screenshot, args=(conn,))
            thread.start()
    finally:
        sock.close()


if __name__ == '__main__':
    main()