import os
from PIL import Image

src_img = r"C:\Users\shaur\.gemini\antigravity-ide\brain\00636412-27eb-4f1d-bcf1-68bb5023bf74\.user_uploaded\media_1787342444463.png"
res_dir = r"d:\VS Code\Android\Back-tap\app\src\main\res"

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

def generate_icons():
    img = Image.open(src_img).convert("RGBA")
    
    # We want to add safe zone padding. 
    # Standard Android adaptive icons have a safe zone of about 66% (diameter 72 out of 108).
    # We will pad the image by about 20% on all sides.
    
    # Calculate target icon size taking padding into account
    for dpi, size in sizes.items():
        # Create a solid black background image for the final icon
        final_img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
        
        # Calculate padding (e.g. 15% of size on each side)
        padding = int(size * 0.15)
        content_size = size - (padding * 2)
        
        # Resize original image to fit within the padded area
        resized_img = img.resize((content_size, content_size), Image.Resampling.LANCZOS)
        
        # Paste it in the center
        final_img.paste(resized_img, (padding, padding))
        
        # Save standard icon
        dir_path = os.path.join(res_dir, f"mipmap-{dpi}")
        os.makedirs(dir_path, exist_ok=True)
        final_img.save(os.path.join(dir_path, "ic_launcher.png"))
        
        # Save round icon (usually same for simple apps unless we want a circle crop)
        # Let's crop it into a circle
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_img.paste(final_img, (0, 0), mask=mask)
        
        round_img.save(os.path.join(dir_path, "ic_launcher_round.png"))

    print("Icons generated successfully!")

if __name__ == "__main__":
    generate_icons()
