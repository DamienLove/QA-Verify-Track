from PIL import Image, ImageDraw, ImageFont
import math
import os

def create_futuristic_icon(path):
    size = 512
    center = size // 2
    
    # Colors
    bg_color = (5, 5, 16)      # Deep Void Blue
    cyan = (0, 240, 255)       # Cyber Cyan
    dark_cyan = (0, 100, 110)
    magenta = (255, 0, 85)     # Neon Magenta
    white = (255, 255, 255)
    
    img = Image.new('RGB', (size, size), bg_color)
    draw = ImageDraw.Draw(img)
    
    # 1. Background Hex Grid (Subtle)
    hex_radius = 40
    for row in range(-2, 10):
        for col in range(-2, 10):
            x = col * (hex_radius * 1.5)
            y = row * (hex_radius * math.sqrt(3))
            if col % 2 == 1:
                y += (hex_radius * math.sqrt(3)) / 2
            
            # Simple hex approximation (just dots for grid effect)
            draw.regular_polygon((x, y, hex_radius), 6, rotation=30, fill=None, outline=(20, 30, 50))

    # 2. Main Hexagon Shield (Thick glowing outline)
    main_hex_radius = 180
    # Outer glow (simulated with multiple lines)
    for i in range(10, 0, -1):
        opacity = 255 - (i * 20)
        # Note: PIL lines don't support alpha directly on RGB images easily without composite. 
        # We'll stick to solid bands for the "retro" futuristic look or layered drawing.
        # Simple solid outline for this script:
        draw.regular_polygon((center, center, main_hex_radius + i), 6, rotation=30, outline=dark_cyan, width=1)

    # Main sharp outline
    draw.regular_polygon((center, center, main_hex_radius), 6, rotation=30, outline=cyan, width=8)
    
    # Inner fill (slightly lighter than bg)
    draw.regular_polygon((center, center, main_hex_radius - 10), 6, rotation=30, fill=(10, 20, 40))

    # 3. Tech Circuitry / HUD Elements
    # Top-Left to Center
    draw.line([(0, 0), (100, 100), (center - 50, center - 50)], fill=cyan, width=3)
    draw.ellipse((center - 55, center - 55, center - 45, center - 45), fill=cyan)
    
    # Bottom-Right to Center
    draw.line([(size, size), (size - 100, size - 100), (center + 50, center + 50)], fill=magenta, width=3)
    draw.ellipse((center + 45, center + 45, center + 55, center + 55), fill=magenta)

    # Corner Brackets
    bracket_len = 60
    margin = 40
    t = 6
    # TL
    draw.line([(margin, margin), (margin + bracket_len, margin)], fill=cyan, width=t)
    draw.line([(margin, margin), (margin, margin + bracket_len)], fill=cyan, width=t)
    # BR
    draw.line([(size - margin, size - margin), (size - margin - bracket_len, size - margin)], fill=cyan, width=t)
    draw.line([(size - margin, size - margin), (size - margin, size - margin - bracket_len)], fill=cyan, width=t)

    # 4. Typography ("QA")
    try:
        # Try to find a font that looks somewhat techy, or fallback
        font_size = 180
        font = ImageFont.truetype("arial.ttf", font_size)
    except:
        font = ImageFont.load_default()

    text = "QA"
    bbox = draw.textbbox((0, 0), text, font=font)
    w = bbox[2] - bbox[0]
    h = bbox[3] - bbox[1]
    
    # Draw Text Shadow/Glow
    draw.text(((size - w) / 2 + 4, (size - h) / 2 - 20 + 4), text, font=font, fill=dark_cyan)
    # Draw Main Text
    draw.text(((size - w) / 2, (size - h) / 2 - 20), text, font=font, fill=white)

    # 5. Scanline Overlay (Optional detail)
    for y in range(0, size, 4):
        draw.line([(0, y), (size, y)], fill=(0, 0, 0), width=1)

    img.save(path)
    print(f"Futuristic icon saved to {path}")

if __name__ == "__main__":
    output_path = "play_console_assets/futuristic_icon.png"
    create_futuristic_icon(output_path)
