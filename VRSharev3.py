import socket
from threading import Thread
import ctypes
import io
from PIL import ImageGrab
from PIL import Image
import win32gui


user32 = ctypes.windll.user32
ctypes.windll.user32.SetProcessDPIAware()
WIDTH = user32.GetSystemMetrics(0)
HEIGHT = user32.GetSystemMetrics(1)
#imCursor = Image.open('cursor.png')


def get_cfg():
    with open('cfg.txt','r') as f:
        lines = f.readlines()
        ip = lines[0].split('=')[1].replace('\n','')
        res = lines[1].split('=')[1].replace('\n','').split('x')
        qual = lines[2].split('=')[1]
    return ip, (int(res[0]), int(res[1])), int(qual)

def retreive_screenshot(conn,res, qual):
    while True:
        try:
            curX,curY=win32gui.GetCursorPos()
            output = io.BytesIO()
            img = ImageGrab.grab()
            #img.paste(imCursor,box=(curX,curY),mask=imCursor)
            img = img.resize(res,Image.ANTIALIAS)
            img.save(output,'jpeg',quality=qual)
            img_png = output.getvalue()
            size = len(img_png)
            size_division = (size // 256)
            conn.send(size_division.to_bytes(2,'big') + size.to_bytes(4,'big') + img_png)
        except:
            break
def main(port=5000):
    connected = False
    ip, res, qual = get_cfg()
    sock = socket.socket()      
    sock.bind((ip, port)) 
    try:
        sock.listen()
        print('Server started.')

        while 'connected':
            conn, addr = sock.accept()
            if(connected == False):
                print('Client connected IP:', addr)
                connected = True
            thread = Thread(target=retreive_screenshot, args=(conn,res,qual))
            thread.start()
    finally:
        sock.close()
        connected = False


if __name__ == '__main__':
    main()