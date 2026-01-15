import os
from PIL import Image

def update_icons():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    source_icon_path = os.path.join(script_dir, "..", "..", "branding", "qavt_01_shield_check_512.png")
    res_path = os.path.join(script_dir, "..", "app", "src", "main", "res")
    
    if not os.path.exists(source_icon_path):
        print(f"Error: Source icon not found at {source_icon_path}")
        return

    icon = Image.open(source_icon_path)
    
    # Standard mipmap sizes for launcher icons
    # mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    for folder, size in densities.items():
        folder_path = os.path.join(res_path, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Resize and save ic_launcher.png (square/legacy)
        resized_icon = icon.resize((size, size), Image.Resampling.LANCZOS)
        resized_icon.save(os.path.join(folder_path, "ic_launcher.png"))
        
        # Resize and save ic_launcher_round.png (round)
        # Note: Ideally this would be masked to a circle, but for a dev icon, 
        # reusing the square or just letting Android mask it is often acceptable. 
        # However, to be "round", let's apply a basic circle mask to be "correct".
        
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        round_icon = resized_icon.copy()
        round_icon.putalpha(mask)
        round_icon.save(os.path.join(folder_path, "ic_launcher_round.png"))
        
        print(f"Generated icons for {folder} ({size}x{size})")

if __name__ == "__main__":
    update_icons()
