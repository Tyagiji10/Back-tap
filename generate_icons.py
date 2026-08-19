import os
import glob
from PIL import Image

src_img_path = r"C:\Users\shaur\.gemini\antigravity-ide\brain\e3c94c8d-2fd4-4ccf-a75e-b5ba516bd4ea\back_tap_app_icon_v2_1787173916716.jpg"
res_dir = r"d:\VS Code\Android\Back-tap\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Remove existing mipmap contents
for folder in os.listdir(res_dir):
    if folder.startswith("mipmap"):
        folder_path = os.path.join(res_dir, folder)
        for f in os.listdir(folder_path):
            os.remove(os.path.join(folder_path, f))

# Generate new icons
img = Image.open(src_img_path).convert("RGBA")

for folder, size in sizes.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    
    resized.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
    resized.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")

print("Icons successfully generated and old ones removed.")
