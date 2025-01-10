from socket import *
from threading import Thread
import ctypes
import io
from PIL import ImageGrab
from PIL import Image

user32 = ctypes.windll.user32
WIDTH = user32.GetSystemMetrics(0)
HEIGHT = user32.GetSystemMetrics(1)


def retreive_screenshot(conn, addr):
        while True:
            output = io.BytesIO()
            img = ImageGrab.grab()
            img = img.resize((800,600),Image.ANTIALIAS)
            img.save(output,'jpeg',quality=50)
            img_png = output.getvalue()
            size = int(len(img_png)/2)
            
            try:
                print(len(img_png[0:size]))
                conn.sendto(img_png[0:size], addr)
                conn.sendto(img_png[size:], addr)
            except:
                pass

def main(host='192.168.0.20', port=5000):
    sock = socket(AF_INET, SOCK_DGRAM)
      
    sock.bind((host, port)) 
    try:
        while 'connected':
            conn, addr = sock.recvfrom(1024)
            print('Client connected IP:', addr)
            retreive_screenshot(sock, addr)
    finally:
        sock.close()


if __name__ == '__main__':
    main()