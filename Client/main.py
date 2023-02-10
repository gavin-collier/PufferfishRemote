import socketio
import tkinter as tk
import webbrowser,string, random
from tkinter.ttk import *
from tkinter import *
from tkinter import messagebox
from PIL import Image, ImageTk
import sys

sio = socketio.client()
sio.connect('http://localhost:8080') #chnage this to webapp after testing
hosting = False

def on_closing():
    #if the user clicks confirm distroy the window
    if messagebox.askokcancel("Quit", "Do you want to quit?"):
        sio.disconnect()
        root.destroy()

def generate_random_string(length):
    chars = string.ascii_uppercase + string.digits
    return ''.join(random.choice(chars) for _ in range(length))

def generate_room():
    room_id = generate_random_string(6)
    sio.emit('createRoom', room_id)
    return room_id

def new_room():
    global hosting
    if hosting == False:
        hosting = True
        newWindow = Toplevel(root)
        room_id = generate_room()

        #tkinter GUI Code
        newWindow.geometry("400x400")
        newWindow.title("Host Room")
        newWindow.protocol("WM_DELETE_WINDOW", on_closing)

        # Add bg image file
        bg2 = Image.open("client/files/bg.png")
        resize_bg2 = bg2.resize((400,400))
        bg2 = ImageTk.PhotoImage(resize_bg)
        bg_label2 = tk.Label(newWindow, image = bg)
        bg_label2.place(x = 0, y = 0)

        tk.Label(
        newWindow,
        text="Room ID: " + room_id,
        bg="#000",
        fg="white",
        font=("Oswald", 25)).grid(row = 0, column = 0, pady=20, padx=(60, 0))

#tkinter functions
def open_github():
    webbrowser.open('https://github.com/gavin-collier/PufferfishRemote')

def open_pufferfish():
    webbrowser.open('https://pufferfish-web-rmbmdewxta-wn.a.run.app/') #change this to the web URL is done

#tkinter GUI Code
root = tk.Tk()
root.geometry("400x400")
root.protocol("WM_DELETE_WINDOW", on_closing)
root.title("Pufferfish")
# logo photo
icon = tk.PhotoImage(file = 'client/files/pufferfish.png')
root.iconphoto = (True, icon)
root.resizable(False, False) 
root.configure(bg="#333")


# Add bg image file
bg = Image.open("client/files/bg.png")
resize_bg = bg.resize((400,400))
bg = ImageTk.PhotoImage(resize_bg)
bg_label = tk.Label(root, image = bg)
bg_label.place(x = 0, y = 0)

tk.Label(
    text="Pufferfish Client",
    bg="#000",
    fg="white",
    font=("Oswald", 25)).grid(row = 0, column = 0, pady=20, padx=(60, 0))
# The logo
image = Image.open("client/files/pufferfish.png")
resize_image = image.resize((50, 50))
img = ImageTk.PhotoImage(resize_image)
logo_label = tk.Label(image=img)
logo_label.image = img
logo_label.grid(row = 0, column = 1, padx=20)

create_room_button = tk.Button(root, text="Create Room", command=new_room, bg="#555", fg="#fff", width=20, height=2)
create_room_button.place(relx = 0.5, rely = 0.35, anchor="center")

github_img_resize = Image.open("client/files/github.png")
github_img_resize = github_img_resize.resize((15, 15))
github_img = ImageTk.PhotoImage(github_img_resize)
github_button = tk.Button(root, image=github_img, command=open_github, bg="#555", fg="#fff", width=15, height=15)
github_button.place(relx = 0.4, rely = 0.9, anchor="center")

logo_resize = image.resize((15,15))
logo_img = ImageTk.PhotoImage(logo_resize)
webpage_button = tk.Button(root, image=logo_img, command=open_pufferfish, bg="#555", fg="#fff", width=15, height=15)
webpage_button.place(relx=0.6, rely=0.9, anchor="center")

root.mainloop()