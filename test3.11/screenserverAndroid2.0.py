import socket
from concurrent.futures import ThreadPoolExecutor
import io
from PIL import ImageGrab, Image
import win32gui
import keyboard
import traceback

cursor = True
imCursor = Image.open('cursor.png')

def retrieve_screenshot(conn):
    global imCursor
    global cursor
    
    try:
        while True:
            img = ImageGrab.grab()
            
            if keyboard.is_pressed('ctrl+alt+1'):
                cursor = False
            if keyboard.is_pressed('ctrl+alt+2'):
                cursor = True
            
            if cursor:
                curX, curY = win32gui.GetCursorPos()
                img.paste(imCursor, box=(curX, curY), mask=imCursor)
            
            img = img.resize((2560, 1440))
            
            output = io.BytesIO()
            img.save(output, 'jpeg', quality=70)
            img_png = output.getvalue()
            
            size = len(img_png)
            #print(size)
            conn.send(size.to_bytes(4, 'big') + img_png)
    except Exception as e:
        pass

def handle_client(conn):
    print(f'Client connected IP: {conn.getpeername()}')
    retrieve_screenshot(conn)

def main(port=5000):
    sock = socket.socket()
    sock.bind(("192.168.1.172", port))
    sock.listen()
    print('Server started.')

    with ThreadPoolExecutor() as executor:
        while True:
            conn, _ = sock.accept()
            executor.submit(handle_client, conn)

if __name__ == '__main__':
    main()
