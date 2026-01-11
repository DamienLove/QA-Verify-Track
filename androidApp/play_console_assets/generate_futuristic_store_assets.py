from PIL import Image, ImageDraw, ImageFont
import math
import os
import random

# Colors
BG_COLOR = (5, 5, 10)
CYAN = (0, 240, 255)
DARK_CYAN = (0, 80, 90)
RED = (255, 30, 60)
GREEN = (0, 255, 100)
WHITE = (240, 250, 255)
GLASS_BG = (20, 30, 50) # Semi-transparent effect (simulated)

def draw_hex_grid(draw, w, h, size=50, color=(15, 25, 40)):
    for row in range(-1, int(h/size) + 2):
        for col in range(-1, int(w/(size*1.5)) + 2):
            x = col * (size * 1.5)
            y = row * (size * math.sqrt(3))
            if col % 2 == 1:
                y += (size * math.sqrt(3)) / 2
            
            # Draw hex points (simplified)
            draw.regular_polygon((x, y, size-2), 6, rotation=30, outline=color)

def draw_hud_panel(draw, x, y, w, h, title=None, glow=False):
    # Main panel bg (simulated glass)
    draw.rectangle([x, y, x+w, y+h], fill=GLASS_BG, outline=None)
    
    # Border with "Tech" corners
    corner_len = 20
    border_col = CYAN if glow else DARK_CYAN
    
    # Draw cut corners
    points = [
        (x + corner_len, y), (x + w - corner_len, y),
        (x + w, y + corner_len), (x + w, y + h - corner_len),
        (x + w - corner_len, y + h), (x + corner_len, y + h),
        (x, y + h - corner_len), (x, y + corner_len)
    ]
    draw.polygon(points, outline=border_col, width=2)
    
    if title:
        try:
            font = ImageFont.truetype("arial.ttf", 24)
        except:
            font = ImageFont.load_default()
        
        # Header bar
        draw.polygon([(x, y + corner_len), (x + corner_len, y), (x + w - corner_len, y), (x + w, y + corner_len), (x+w, y+40), (x, y+40)], fill=DARK_CYAN)
        draw.text((x + 20, y + 8), title.upper(), fill=WHITE, font=font)

def create_feature_graphic(path):
    w, h = 1024, 500
    img = Image.new('RGB', (w, h), BG_COLOR)
    draw = ImageDraw.Draw(img)
    
    draw_hex_grid(draw, w, h, size=60, color=(20, 30, 50))
    
    # Central "Core"
    cx, cy = w//2, h//2
    draw.regular_polygon((cx, cy, 150), 6, rotation=0, outline=DARK_CYAN, width=4)
    draw.regular_polygon((cx, cy, 130), 6, rotation=0, outline=CYAN, width=2)
    
    # Title
    try:
        title_font = ImageFont.truetype("arial.ttf", 70)
        sub_font = ImageFont.truetype("arial.ttf", 30)
    except:
        title_font = ImageFont.load_default()
        sub_font = ImageFont.load_default()
        
    text = "QA VERIFY & TRACK"
    bbox = draw.textbbox((0, 0), text, font=title_font)
    tw = bbox[2]-bbox[0]
    draw.text(((w-tw)/2, cy - 40), text, font=title_font, fill=WHITE)
    
    sub = "// ADVANCED BUILD TELEMETRY //"
    s_bbox = draw.textbbox((0, 0), sub, font=sub_font)
    sw = s_bbox[2]-s_bbox[0]
    draw.text(((w-sw)/2, cy + 50), sub, font=sub_font, fill=CYAN)
    
    # Tech lines
    draw.line([(0, 50), (w, 50)], fill=CYAN, width=2)
    draw.line([(0, h-50), (w, h-50)], fill=CYAN, width=2)
    
    img.save(path)
    print(f"Saved {path}")

def create_ui_screen(w, h, title, panel_data):
    img = Image.new('RGB', (w, h), BG_COLOR)
    draw = ImageDraw.Draw(img)
    draw_hex_grid(draw, w, h)
    
    # Top HUD Bar
    draw.rectangle([0, 0, w, 80], fill=(10, 15, 20))
    draw.line([(0, 80), (w, 80)], fill=CYAN, width=2)
    
    try:
        header_font = ImageFont.truetype("arial.ttf", 40)
        font = ImageFont.truetype("arial.ttf", 30)
    except:
        header_font = ImageFont.load_default()
        font = ImageFont.load_default()
        
    draw.text((40, 20), title.upper(), font=header_font, fill=WHITE)
    draw.text((w - 200, 25), "SYS: ONLINE", font=font, fill=GREEN)
    
    # Panels
    for panel in panel_data:
        px, py, pw, ph, ptitle, content_lines = panel
        draw_hud_panel(draw, px, py, pw, ph, ptitle)
        
        ly = py + 60
        for line in content_lines:
            col = WHITE
            if "[OK]" in line: col = GREEN
            elif "[ERR]" in line: col = RED
            elif "[WARN]" in line: col = (255, 200, 0)
            elif ">>" in line: col = CYAN
            
            draw.text((px + 20, ly), line, font=font, fill=col)
            ly += 40

    return img

def create_screenshots(output_dir):
    # Phone 1: Command Center
    phone1 = create_ui_screen(1080, 1920, "Command Center", [
        (50, 150, 980, 300, "Repository Index", [
            ">> Android-App-Core [OK]",
            ">> Backend-API-Node [WARN]",
            ">> Web-Client-React [OK]",
            ">> Auth-Service-GCP [OK]"
        ]),
        (50, 500, 980, 400, "Active Telemetry", [
            "Build #8849: COMPILING...",
            "Cpu Load: 45%",
            "Memory: 12GB / 32GB",
            "Network: STABLE"
        ]),
        (50, 950, 980, 200, "Quick Actions", [
            "[ INITIATE BUILD ]",
            "[ VIEW LOGS ]"
        ])
    ])
    phone1.save(os.path.join(output_dir, "futuristic_phone_1.png"))
    
    # Phone 2: Details
    phone2 = create_ui_screen(1080, 1920, "Data Node: Android-Core", [
        (50, 150, 980, 500, "Build Diagnostics", [
            "Status: SUCCESS",
            "Version: 2.1.0-RC4",
            "Branch: feature/cyber-ui",
            "Commit: 8f3a21 (Verified)",
            "Artifacts: APK, AAB"
        ]),
        (50, 700, 980, 300, "QA Protocol", [
            "Unit Tests: 142/142 PASSED",
            "UI Tests: 50/50 PASSED",
            "Security Scan: CLEAN"
        ])
    ])
    phone2.save(os.path.join(output_dir, "futuristic_phone_2.png"))
    
    # Tablet 7"
    tab7 = create_ui_screen(1280, 800, "Tactical Overview", [
        (50, 100, 500, 600, "Project List", ["> Alpha [OK]", "> Beta [OK]", "> Gamma [ERR]"]),
        (600, 100, 630, 600, "Detail View", ["Select a node to view telemetry.", "System awaiting input..."])
    ])
    tab7.save(os.path.join(output_dir, "futuristic_tablet_7.png"))
    
    # Tablet 10"
    tab10 = create_ui_screen(1920, 1200, "Global Overwatch", [
        (50, 150, 550, 400, "Server Health", ["US-East: OK", "EU-West: OK", "Asia-South: OK"]),
        (650, 150, 550, 400, "Deployment Queue", ["Job 112: Pending", "Job 113: Pending"]),
        (1250, 150, 600, 900, "Live Log", ["10:00: Sys init", "10:01: Connected", "10:02: Data sync"]),
        (50, 600, 1150, 450, "Build Matrix", ["Android: 98%", "iOS: 95%", "Web: 100%"])
    ])
    tab10.save(os.path.join(output_dir, "futuristic_tablet_10.png"))
    
    print("Saved screenshots")

if __name__ == "__main__":
    out_dir = "play_console_assets"
    os.makedirs(out_dir, exist_ok=True)
    
    create_feature_graphic(os.path.join(out_dir, "futuristic_feature_graphic.png"))
    create_screenshots(out_dir)
