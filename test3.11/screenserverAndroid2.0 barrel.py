import socket
from threading import Thread
import io
from PIL import ImageGrab
from PIL import Image
import win32gui
import keyboard
import time
from wand.image import Image as im
import numpy as np

imCursor = Image.open('cursor.png')
cursor = True

def get_cfg():
    with open('cfg.txt','r') as f:
        lines = f.readlines()
        ip = lines[0].split('=')[1].replace('\n','')
        res = lines[1].split('=')[1].replace('\n','').split('x')
        qual = lines[2].split('=')[1]
    return ip, (int(res[0]), int(res[1])), int(qual)

def retreive_screenshot(conn,res, qual):
    global cursor 
    loop = range(2147483647)
    for _ in loop:
        try:
            #start_t = time.perf_counter()
            img = ImageGrab.grab()
            if keyboard.is_pressed('ctrl+alt+1'):
                cursor = False
            if keyboard.is_pressed('ctrl+alt+2'):
                cursor = True
            if cursor == True:
                curX,curY=win32gui.GetCursorPos()
                img.paste(imCursor,box=(curX,curY),mask=imCursor)
            output = io.BytesIO()
            img = img.resize((768,768),Image.NEAREST) 
            img = im.from_array(np.array(img))
            img.virtual_pixel = 'transparent'
            img.distort('barrel', (0.2, 0.0, 0.0, 1.0))
            img = Image.fromarray(np.array(img))
            img = img.convert("RGB")
            img.save(output,'jpeg',quality=60)
            img_png = output.getvalue()
            size = len(img_png)
            #conn.send(img_png)
            conn.send(size.to_bytes(4,'big') + img_png)
            #end_t = time.perf_counter()
            #wall_duration = end_t - start_t
            #print(f"it took: {wall_duration:.4f}s")
        except Exception as e:
            print(e)
            break
    return
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