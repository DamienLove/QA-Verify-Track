from PIL import Image, ImageDraw, ImageFont
import os
import random

def draw_grid(draw, width, height, color=(0, 40, 80), step=64):
    for x in range(0, width, step):
        draw.line([(x, 0), (x, height)], fill=color)
    for y in range(0, height, step):
        draw.line([(0, y), (width, y)], fill=color)

def draw_debug_info(draw, width, height, text_color=(100, 255, 100)):
    # Corners
    length = 40
    t = 4
    # Top-Left
    draw.line([(0, 0), (length, 0)], fill=text_color, width=t)
    draw.line([(0, 0), (0, length)], fill=text_color, width=t)
    # Bottom-Right
    draw.line([(width, height-t), (width-length, height-t)], fill=text_color, width=t)
    draw.line([(width-t, height), (width-t, height-length)], fill=text_color, width=t)
    
    # Resolution Text
    try:
        font = ImageFont.truetype("arial.ttf", 30)
    except:
        font = ImageFont.load_default()
    
    label = f"{width}x{height} // DEV_ASSET"
    draw.text((20, height - 50), label, fill=text_color, font=font)

def create_feature_graphic(path):
    w, h = 1024, 500
    bg = (10, 10, 15)
    img = Image.new('RGB', (w, h), bg)
    draw = ImageDraw.Draw(img)
    
    draw_grid(draw, w, h)
    draw_debug_info(draw, w, h)
    
    # Title
    try:
        title_font = ImageFont.truetype("arial.ttf", 80)
        sub_font = ImageFont.truetype("arial.ttf", 40)
    except:
        title_font = ImageFont.load_default()
        sub_font = ImageFont.load_default()
        
    text = "QA VERIFY & TRACK"
    bbox = draw.textbbox((0, 0), text, font=title_font)
    tw, th = bbox[2]-bbox[0], bbox[3]-bbox[1]
    draw.text(((w-tw)/2, (h-th)/2 - 20), text, font=title_font, fill=(255, 255, 255))
    
    sub = "[ FEATURE_GRAPHIC_PLACEHOLDER ]"
    s_bbox = draw.textbbox((0, 0), sub, font=sub_font)
    sw, sh = s_bbox[2]-s_bbox[0], s_bbox[3]-s_bbox[1]
    draw.text(((w-sw)/2, (h-th)/2 + 80), sub, font=sub_font, fill=(0, 255, 255))

    img.save(path)
    print(f"Saved {path}")

def create_mock_ui(w, h, title, items):
    bg = (20, 20, 25)
    img = Image.new('RGB', (w, h), bg)
    draw = ImageDraw.Draw(img)
    draw_grid(draw, w, h, (40, 40, 50), 128)
    
    # Status Bar
    draw.rectangle([0, 0, w, 40], fill=(0, 0, 0))
    
    # App Bar
    draw.rectangle([0, 40, w, 160], outline=(0, 200, 255), width=2)
    try:
        font = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
    draw.text((40, 70), title, font=font, fill=(255, 255, 255))
    
    # Mock List Items
    y_start = 180
    item_h = 150
    gap = 20
    
    for i, item_text in enumerate(items):
        y = y_start + i * (item_h + gap)
        if y + item_h > h: break
        
        # Card
        draw.rectangle([40, y, w-40, y+item_h], outline=(100, 100, 100), width=2)
        
        # Placeholder Icon circle
        draw.ellipse([60, y+35, 60+80, y+35+80], outline=(100, 100, 100), width=2)
        
        # Text lines
        draw.line([160, y+50, w-100, y+50], fill=(150, 150, 150), width=20) # Title mock
        draw.line([160, y+100, w-200, y+100], fill=(80, 80, 80), width=15) # Subtitle mock
        
        # Actual debug label
        draw.text((160, y+40), item_text, font=font, fill=(200, 200, 200))

    draw_debug_info(draw, w, h)
    return img

def create_screenshots(output_dir):
    # Phone 1: Home
    phone_home = create_mock_ui(1080, 1920, "My Repositories", ["Repo: Android-App", "Repo: Backend-API", "Repo: Web-Client", "Repo: Auth-Service"])
    phone_home.save(os.path.join(output_dir, "phone_screenshot_1.png"))
    
    # Phone 2: Details
    phone_detail = create_mock_ui(1080, 1920, "Repo Details", ["Status: CONNECTED", "Branch: main", "Last Build: #4242", "QA Status: PENDING", "Tests: 45/50 Passing"])
    phone_detail.save(os.path.join(output_dir, "phone_screenshot_2.png"))
    
    # Tablet 7" (1280x800)
    tab7 = create_mock_ui(1280, 800, "Dashboard (Tablet 7)", ["Project Alpha", "Project Beta", "Project Gamma"])
    tab7.save(os.path.join(output_dir, "tablet_7_screenshot.png"))
    
    # Tablet 10" (1920x1200)
    tab10 = create_mock_ui(1920, 1200, "Global Overview (Tablet 10)", ["Service A [ONLINE]", "Service B [OFFLINE]", "Service C [MAINTENANCE]", "Cluster Status: OK"])
    tab10.save(os.path.join(output_dir, "tablet_10_screenshot.png"))
    
    print("Saved screenshots")

if __name__ == "__main__":
    out_dir = "play_console_assets"
    os.makedirs(out_dir, exist_ok=True)
    
    create_feature_graphic(os.path.join(out_dir, "feature_graphic.png"))
    create_screenshots(out_dir)
