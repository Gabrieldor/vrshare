import socket
from threading import Thread
import io
from PIL import ImageGrab
from PIL import Image
import win32gui
import keyboard
import time
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

def barrel_distortion(image):
    # Get the size of the input image
    width, height = image.size

    # Define the parameters for the barrel distortion
    k = 0.5  # Distortion factor
    cx = width / 2  # X-coordinate of center point
    cy = height / 2  # Y-coordinate of center point

    # Create a mesh grid of pixel coordinates
    x, y = np.meshgrid(np.arange(width), np.arange(height))

    # Convert the mesh grid to polar coordinates
    r = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
    theta = np.arctan2(y - cy, x - cx)

    # Apply the barrel distortion equation
    r_distorted = r * (1 + k * r ** 2)

    # Convert back to Cartesian coordinates
    x_distorted = r_distorted * np.cos(theta) + cx
    y_distorted = r_distorted * np.sin(theta) + cy

    # Create a new image with the distorted pixel values
    distorted_image = Image.new(image.mode, (width, height))
    pixels = distorted_image.load()
    for i in range(width):
        for j in range(height):
            x_new = int(x_distorted[j, i])
            y_new = int(y_distorted[j, i])
            if x_new >= 0 and x_new < width and y_new >= 0 and y_new < height:
                pixels[i, j] = image.getpixel((x_new, y_new))

    return distorted_image

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
            img = barrel_distortion(img)
            output = io.BytesIO()
            img = img.resize((1170,1080),Image.NEAREST) 
            img.save(output,'jpeg',quality=60)
            img_png = output.getvalue()
            size = len(img_png)
            conn.send(size.to_bytes(4,'big') + img_png)
            #end_t = time.perf_counter()
            #wall_duration = end_t - start_t
            #print(f"it took: {wall_duration:.4f}s")
        except:
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