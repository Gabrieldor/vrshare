from mss import mss
from zlib import compress
from zlib import decompress
import pygame
import os
import ctypes

user32 = ctypes.windll.user32
half_tel_width = int((user32.GetSystemMetrics(78) + user32.GetSystemMetrics(76) - user32.GetSystemMetrics(0))/2)
WIDTH = user32.GetSystemMetrics(0)
HEIGHT = user32.GetSystemMetrics(1)

with mss() as sct:
    os.environ['SDL_VIDEO_WINDOW_POS'] = "%d,%d" % (WIDTH,0)
    pygame.init()
    screen = pygame.display.set_mode((half_tel_width*2,HEIGHT))
    clock = pygame.time.Clock() 
    watching = True

    while watching:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                watching = False
                break

        # Retreive the size of the pixels length, the pixels length and pixels

        # Create the Surface from raw pixels
        rect = {'top': 0, 'left': 0, 'width': WIDTH, 'height': HEIGHT}
        imga = sct.grab(rect)
        pixels = compress(imga.rgb, 0)
        aa = decompress(pixels)

        img = pygame.image.fromstring(aa, (WIDTH, HEIGHT), 'RGB')
        img = pygame.transform.scale(img, (half_tel_width, 1152))

        # Display the picture
        screen.blit(img, (0, 0))
        screen.blit(img, (half_tel_width, 0))
        
        pygame.display.flip()
        clock.tick(360)

        os.environ['SDL_VIDEO_WINDOW_POS'] = "%d,%d" % (WIDTH+half_tel_width,0)
        pygame.init()
