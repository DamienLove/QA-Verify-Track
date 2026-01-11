from PIL import Image, ImageDraw, ImageFont
import os

def create_dev_icon(path):
    size = 512
    # Colors
    bg_color = (32, 32, 32) # Dark Grey
    grid_color = (64, 64, 64) # Lighter Grey
    accent_color = (0, 255, 0) # Neon Green
    text_color = (255, 255, 255) # White
    border_color = (255, 0, 255) # Magenta (Debug color)

    # Create image
    img = Image.new('RGB', (size, size), bg_color)
    draw = ImageDraw.Draw(img)

    # Draw Grid (64px)
    grid_size = 64
    for i in range(0, size, grid_size):
        draw.line([(i, 0), (i, size)], fill=grid_color, width=1)
        draw.line([(0, i), (size, i)], fill=grid_color, width=1)

    # Draw Border
    border_width = 8
    draw.rectangle([0, 0, size-1, size-1], outline=border_color, width=border_width)

    # Draw Center Crosshair
    mid = size // 2
    draw.line([(mid - 20, mid), (mid + 20, mid)], fill=accent_color, width=2)
    draw.line([(mid, mid - 20), (mid, mid + 20)], fill=accent_color, width=2)

    # Draw Text (Fallback to basic drawing if font loading fails, but try to load default)
    try:
        # Try to load a monospace font, or default
        font_size = 120
        try:
            font = ImageFont.truetype("arial.ttf", font_size)
        except IOError:
            font = ImageFont.load_default()
        
        text = "DEV"
        # Calculate text position (basic centering)
        text_bbox = draw.textbbox((0, 0), text, font=font)
        text_w = text_bbox[2] - text_bbox[0]
        text_h = text_bbox[3] - text_bbox[1]
        
        draw.text(((size - text_w) / 2, (size - text_h) / 2 - 20), text, font=font, fill=text_color)
        
        # Subtext
        sub_font_size = 40
        try:
            sub_font = ImageFont.truetype("arial.ttf", sub_font_size)
        except IOError:
            sub_font = ImageFont.load_default()
            
        sub_text = "BUILD"
        sub_bbox = draw.textbbox((0, 0), sub_text, font=sub_font)
        sub_w = sub_bbox[2] - sub_bbox[0]
        
        draw.text(((size - sub_w) / 2, (size - text_h) / 2 + 100), sub_text, font=sub_font, fill=accent_color)

    except Exception as e:
        print(f"Text drawing failed: {e}")

    # Save
    img.save(path)
    print(f"Dev icon saved to {path}")

if __name__ == "__main__":
    output_path = "play_console_assets/dev_style_icon.png"
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    create_dev_icon(output_path)
