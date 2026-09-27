import cv2
import numpy as np
import math
import os
import time
from PIL import Image, ImageDraw, ImageFont

def render_ravex_intro(output_path, width=3840, height=2160, fps=60, total_frames=160):
    start_time = time.time()
    
    BG_COLOR = np.array([14, 15, 20], dtype=np.uint8)

    logo_path = r'C:\Users\tenen\RaveX\src\main\resources\assets\ravex\textures\ravexv2.png'
    logo_raw = cv2.imread(logo_path, cv2.IMREAD_UNCHANGED)
    h_raw, w_raw = logo_raw.shape[:2]

    target_h = int(360 * (height / 1080.0))
    target_w = int(w_raw * (target_h / h_raw))

    mask_raw = logo_raw[:, :, 3]

    inpaint_mask = (mask_raw == 0).astype(np.uint8)
    color_inpainted = cv2.inpaint(logo_raw[:, :, :3], inpaint_mask, 3, cv2.INPAINT_TELEA)

    dist_in = cv2.distanceTransform(mask_raw, cv2.DIST_L2, 5)
    dist_out = cv2.distanceTransform(255 - mask_raw, cv2.DIST_L2, 5)
    sdf = dist_in - dist_out

    sdf_smooth = cv2.GaussianBlur(sdf, (5, 5), 1.1)

    sdf_scaled = cv2.resize(sdf_smooth, (target_w, target_h), interpolation=cv2.INTER_CUBIC)
    scale_ratio = target_h / float(h_raw)
    edge_width = 1.05 * scale_ratio

    alpha_smooth = np.clip((sdf_scaled / edge_width) + 0.5, 0.0, 1.0)
    color_scaled = cv2.resize(color_inpainted, (target_w, target_h), interpolation=cv2.INTER_LANCZOS4)

    logo_full = np.zeros((target_h, target_w, 4), dtype=np.uint8)
    logo_full[:, :, :3] = color_scaled
    logo_full[:, :, 3] = (alpha_smooth * 255).astype(np.uint8)

    _, thresh_ss = cv2.threshold((alpha_smooth * 255).astype(np.uint8), 128, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(thresh_ss, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
    contours_sorted = sorted(contours, key=cv2.contourArea, reverse=True)

    cnt_r = cv2.approxPolyDP(contours_sorted[0], 1.2, True)
    cnt_w0 = cv2.approxPolyDP(contours_sorted[1], 1.2, True) if len(contours_sorted) > 1 else contours_sorted[0]
    cnt_w1 = cv2.approxPolyDP(contours_sorted[2], 1.2, True) if len(contours_sorted) > 2 else None

    font_base_size = int(135 * (height / 1080.0))
    min_size = int(font_base_size * 0.6)
    max_size = int(font_base_size * 1.6)
    font_cache = {s: ImageFont.truetype(r'C:\Windows\Fonts\GOTHICB.TTF', s) for s in range(min_size, max_size + 1)}
    font_main = font_cache[font_base_size]

    bbox_upper = font_main.getbbox('RaveX')
    h_text = bbox_upper[3] - bbox_upper[1]
    text_gap = int(36 * (height / 1080.0))
    total_content_h = target_h + text_gap + h_text
    logo_y = (height - total_content_h) // 2
    logo_x = (width - target_w) // 2

    center_logo_x = logo_x + target_w // 2
    center_logo_y = logo_y + target_h // 2

    w_r = font_main.getlength('r')
    w_R = font_main.getlength('R')
    w_a = font_main.getlength('a')
    w_v = font_main.getlength('v')
    w_e = font_main.getlength('e')
    w_x = font_main.getlength('x')
    w_X = font_main.getlength('X')

    w_ave = w_a + w_v + w_e
    total_w_lower = w_r + w_ave + w_x
    total_w_upper = w_R + w_ave + w_X

    text_y = logo_y + target_h + text_gap - bbox_upper[1]
    baseline_y = text_y + bbox_upper[3]

    x0_lower = (width - total_w_lower) / 2.0
    x0_upper = (width - total_w_upper) / 2.0

    pos_a_lower = x0_lower + w_r
    pos_a_upper = x0_upper + w_R

    diag = int(math.hypot(target_w, target_h)) + int(60 * (height / 1080.0))
    pad_x = (diag - target_w) // 2
    pad_y = (diag - target_h) // 2
    padded_logo = np.zeros((diag, diag, 4), dtype=np.uint8)
    padded_logo[pad_y:pad_y+target_h, pad_x:pad_x+target_w] = logo_full

    stroke_thickness = max(2, int(7 * (height / 1080.0)))
    ring_thickness = max(2, int(4 * (height / 1080.0)))

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    writer = cv2.VideoWriter(output_path, fourcc, fps, (width, height))

    def ease_out_cubic(t):
        return 1.0 - math.pow(1.0 - t, 3.0)

    def ease_in_out_cubic(t):
        return 4.0 * t * t * t if t < 0.5 else 1.0 - math.pow(-2.0 * t + 2.0, 3.0) / 2.0

    def ease_out_back(t, s=1.70158):
        return 1.0 + (s + 1.0) * math.pow(t - 1.0, 3.0) + s * math.pow(t - 1.0, 2.0)

    def draw_vector_glyph(draw_obj, ch, cx, b_y, scale_val, color_rgb, alpha=1.0):
        s = max(min_size, min(max_size, int(round(font_base_size * scale_val))))
        f = font_cache[s]
        bbox = f.getbbox(ch)
        w = f.getlength(ch)
        yt = b_y - bbox[3]
        xl = cx - w / 2.0
        a = int(max(0, min(255, round(255 * alpha))))
        draw_obj.text((xl, yt), ch, font=f, fill=(color_rgb[0], color_rgb[1], color_rgb[2], a))

    print(f'Rendering {total_frames} frames ({width}x{height} @ {fps}fps)...')

    COL_WHITE = (215, 230, 255)
    COL_BLUE = (28, 144, 255)

    for f in range(total_frames):
        canvas = np.full((height, width, 3), BG_COLOR, dtype=np.uint8)

        if f <= 48:
            t_spin = ease_out_cubic(min(1.0, f / 40.0))
            
            ring_r1 = int((140 + 70 * t_spin) * (height / 1080.0))
            ring_r2 = int((200 + 40 * t_spin) * (height / 1080.0))
            ring_alpha = max(0.0, 1.0 - (f / 45.0))

            if ring_alpha > 0.05:
                angle_r1 = t_spin * 190.0
                for a in range(0, 360, 60):
                    start_a = angle_r1 + a
                    end_a = start_a + 35
                    col_r1 = (int(255 * ring_alpha), int(144 * ring_alpha), int(28 * ring_alpha))
                    cv2.ellipse(canvas, (center_logo_x, center_logo_y), (ring_r1, ring_r1), 0, start_a, end_a, col_r1, ring_thickness, cv2.LINE_AA)

                angle_r2 = -t_spin * 230.0
                for a in range(0, 360, 45):
                    start_a = angle_r2 + a
                    end_a = start_a + 22
                    col_r2 = (int(255 * ring_alpha), int(255 * ring_alpha), int(255 * ring_alpha))
                    cv2.ellipse(canvas, (center_logo_x, center_logo_y), (ring_r2, ring_r2), 0, start_a, end_a, col_r2, max(1, ring_thickness - 1), cv2.LINE_AA)

            t_stroke = ease_out_cubic(min(1.0, f / 32.0))

            n_w0 = int(len(cnt_w0) * min(1.0, t_stroke * 1.08))
            if n_w0 > 1:
                pts_w0 = cnt_w0[:n_w0] + np.array([logo_x, logo_y])
                cv2.polylines(canvas, [pts_w0], False, (255, 255, 255), stroke_thickness, cv2.LINE_AA)

            if t_stroke > 0.45 and cnt_w1 is not None:
                t_w1 = (t_stroke - 0.45) / 0.55
                n_w1 = int(len(cnt_w1) * min(1.0, t_w1))
                if n_w1 > 1:
                    pts_w1 = cnt_w1[:n_w1] + np.array([logo_x, logo_y])
                    cv2.polylines(canvas, [pts_w1], False, (255, 255, 255), stroke_thickness, cv2.LINE_AA)

            n_r = int(len(cnt_r) * min(1.0, t_stroke * 1.05))
            if n_r > 1:
                pts_r = cnt_r[:n_r] + np.array([logo_x, logo_y])
                cv2.polylines(canvas, [pts_r], False, (255, 144, 28), stroke_thickness, cv2.LINE_AA)

        if f >= 18:
            if f < 52:
                t_land = (f - 18) / 34.0
                r_orbit = int(65.0 * (height / 1080.0) * math.pow(1.0 - t_land, 2.2))
                orbit_angle = (1.0 - t_land) * 220.0
                rot_angle = -110.0 * math.pow(1.0 - t_land, 2.2)
                curr_scale = 0.48 + 0.52 * ease_out_back(t_land, 1.25)
                alpha_logo = min(1.0, (f - 18) / 16.0)

                cur_cx = center_logo_x + int(r_orbit * math.cos(math.radians(orbit_angle)))
                cur_cy = center_logo_y + int(r_orbit * math.sin(math.radians(orbit_angle)))

                M = cv2.getRotationMatrix2D((diag / 2.0, diag / 2.0), rot_angle, curr_scale)
                rotated = cv2.warpAffine(padded_logo, M, (diag, diag), flags=cv2.INTER_LANCZOS4, borderMode=cv2.BORDER_CONSTANT, borderValue=(0,0,0,0))
                
                rx0 = cur_cx - diag // 2
                ry0 = cur_cy - diag // 2
                
                c_x1 = max(0, rx0)
                c_y1 = max(0, ry0)
                c_x2 = min(width, rx0 + diag)
                c_y2 = min(height, ry0 + diag)
                
                r_x1 = c_x1 - rx0
                r_y1 = c_y1 - ry0
                r_x2 = r_x1 + (c_x2 - c_x1)
                r_y2 = r_y1 + (c_y2 - c_y1)

                r_patch = rotated[r_y1:r_y2, r_x1:r_x2]
                alpha_p = (r_patch[:, :, 3] / 255.0) * alpha_logo
                for c in range(3):
                    canvas[c_y1:c_y2, c_x1:c_x2, c] = (
                        canvas[c_y1:c_y2, c_x1:c_x2, c] * (1.0 - alpha_p) + r_patch[:, :, c] * alpha_p
                    ).astype(np.uint8)
            else:
                alpha_f = logo_full[:, :, 3] / 255.0
                for c in range(3):
                    roi = canvas[logo_y:logo_y+target_h, logo_x:logo_x+target_w, c]
                    canvas[logo_y:logo_y+target_h, logo_x:logo_x+target_w, c] = (
                        roi * (1.0 - alpha_f) + logo_full[:, :, c] * alpha_f
                    ).astype(np.uint8)

        if f >= 38:
            text_img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
            d_text = ImageDraw.Draw(text_img)
            yt_base = baseline_y - bbox_upper[3]

            if 82 <= f <= 94:
                p_glow = math.sin((f - 82) / 12.0 * math.pi)
                glow_r = int(75 * (height / 1080.0) * (0.8 + 0.4 * p_glow))
                glow_alpha = 0.45 * p_glow
                glow_cx = int(pos_a_upper + w_ave + w_X / 2.0)
                glow_cy = int(baseline_y - (bbox_upper[3] - bbox_upper[1]) / 2.0)
                
                glow_layer = np.zeros((height, width, 3), dtype=np.uint8)
                cv2.circle(glow_layer, (glow_cx, glow_cy), glow_r, (255, 144, 28), -1)
                k_size = int(glow_r * 1.5) | 1
                glow_blur = cv2.GaussianBlur(glow_layer, (k_size, k_size), 0)
                canvas = cv2.addWeighted(canvas, 1.0, glow_blur, glow_alpha, 0)

            if f < 72:
                d_text.text((pos_a_lower, yt_base), 'a', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_lower + w_a, yt_base), 'v', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_lower + w_a + w_v, yt_base), 'e', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((x0_lower, yt_base), 'r', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_lower + w_ave, yt_base), 'x', font=font_main, fill=(COL_BLUE[0], COL_BLUE[1], COL_BLUE[2], 255))

                text_bgr = cv2.cvtColor(np.array(text_img), cv2.COLOR_RGBA2BGRA)
                t_alpha = (text_bgr[:, :, 3] / 255.0).astype(np.float32)

                if f < 62:
                    t_wipe = ease_in_out_cubic((f - 38) / 24.0)
                    soft_w = max(4, int(10 * (height / 1080.0)))
                    wipe_x = int(x0_lower + (total_w_lower + soft_w + 2) * t_wipe)
                    if wipe_x < width:
                        t_alpha[:, wipe_x:] = 0.0
                    for sw in range(soft_w):
                        col = wipe_x - sw
                        if 0 <= col < width:
                            t_alpha[:, col] *= ((soft_w - sw) / float(soft_w))

                t_alpha_3 = np.dstack([t_alpha, t_alpha, t_alpha])
                canvas = (canvas * (1.0 - t_alpha_3) + text_bgr[:, :, :3] * t_alpha_3).astype(np.uint8)

            elif f <= 96:
                t_slide = ease_in_out_cubic(min(1.0, max(0.0, (f - 72) / 22.0)))
                pos_a = (1.0 - t_slide) * pos_a_lower + t_slide * pos_a_upper
                pos_v = pos_a + w_a
                pos_e = pos_v + w_v

                d_text.text((pos_a, yt_base), 'a', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_v, yt_base), 'v', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_e, yt_base), 'e', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))

                cx_r = pos_a - w_r / 2.0
                cx_R = pos_a - w_R / 2.0

                if f < 72:
                    draw_vector_glyph(d_text, 'r', cx_r, baseline_y, 1.0, COL_WHITE, 1.0)
                elif f > 86:
                    draw_vector_glyph(d_text, 'R', cx_R, baseline_y, 1.0, COL_WHITE, 1.0)
                else:
                    p = (f - 72) / 14.0
                    if p < 0.45:
                        sc = 1.0 + 0.18 * (p / 0.45)
                        draw_vector_glyph(d_text, 'r', cx_r, baseline_y, sc, COL_WHITE, 1.0)
                    elif p < 0.55:
                        sc_l = 1.0 + 0.18
                        sc_u = 1.25
                        alpha_u = (p - 0.45) / 0.10
                        draw_vector_glyph(d_text, 'r', cx_r, baseline_y, sc_l, COL_WHITE, 1.0 - alpha_u)
                        draw_vector_glyph(d_text, 'R', cx_R, baseline_y, sc_u, COL_WHITE, alpha_u)
                    else:
                        p_settle = (p - 0.55) / 0.45
                        sc = 1.0 + 0.22 * math.pow(1.0 - p_settle, 2.0)
                        draw_vector_glyph(d_text, 'R', cx_R, baseline_y, sc, COL_WHITE, 1.0)

                cx_x = pos_e + w_e + w_x / 2.0
                cx_X = pos_e + w_e + w_X / 2.0

                if f < 78:
                    draw_vector_glyph(d_text, 'x', cx_x, baseline_y, 1.0, COL_BLUE, 1.0)
                elif f > 94:
                    draw_vector_glyph(d_text, 'X', cx_X, baseline_y, 1.0, COL_BLUE, 1.0)
                else:
                    p = (f - 78) / 16.0
                    if p < 0.45:
                        sc = 1.0 + 0.20 * (p / 0.45)
                        draw_vector_glyph(d_text, 'x', cx_x, baseline_y, sc, COL_BLUE, 1.0)
                    elif p < 0.55:
                        sc_l = 1.0 + 0.20
                        sc_u = 1.28
                        alpha_u = (p - 0.45) / 0.10
                        draw_vector_glyph(d_text, 'x', cx_x, baseline_y, sc_l, COL_BLUE, 1.0 - alpha_u)
                        draw_vector_glyph(d_text, 'X', cx_X, baseline_y, sc_u, COL_BLUE, alpha_u)
                    else:
                        p_settle = (p - 0.55) / 0.45
                        sc = 1.0 + 0.25 * math.pow(1.0 - p_settle, 2.0)
                        draw_vector_glyph(d_text, 'X', cx_X, baseline_y, sc, COL_BLUE, 1.0)

                text_bgr = cv2.cvtColor(np.array(text_img), cv2.COLOR_RGBA2BGRA)
                t_alpha_3 = (text_bgr[:, :, 3:4] / 255.0).astype(np.float32)
                canvas = (canvas * (1.0 - t_alpha_3) + text_bgr[:, :, :3] * t_alpha_3).astype(np.uint8)

            else:
                d_text.text((x0_upper, yt_base), 'R', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_upper, yt_base), 'a', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_upper + w_a, yt_base), 'v', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_upper + w_a + w_v, yt_base), 'e', font=font_main, fill=(COL_WHITE[0], COL_WHITE[1], COL_WHITE[2], 255))
                d_text.text((pos_a_upper + w_ave, yt_base), 'X', font=font_main, fill=(COL_BLUE[0], COL_BLUE[1], COL_BLUE[2], 255))

                text_bgr = cv2.cvtColor(np.array(text_img), cv2.COLOR_RGBA2BGRA)
                t_alpha_3 = (text_bgr[:, :, 3:4] / 255.0).astype(np.float32)
                canvas = (canvas * (1.0 - t_alpha_3) + text_bgr[:, :, :3] * t_alpha_3).astype(np.uint8)

        writer.write(canvas)

    writer.release()
    elapsed = time.time() - start_time
    file_size_mb = os.path.getsize(output_path) / (1024.0 * 1024.0)
    print(f'Rendered: {output_path} ({file_size_mb:.2f} MB, {total_frames} frames) in {elapsed:.1f}s')

if __name__ == '__main__':
    intro_dir = r'C:\Users\tenen\RaveX\intro'
    os.makedirs(intro_dir, exist_ok=True)
    
    render_ravex_intro(os.path.join(intro_dir, 'ravex_intro_1080p.mp4'), 1920, 1080, 60, 160)
    
    render_ravex_intro(os.path.join(intro_dir, 'ravex_intro_4k.mp4'), 3840, 2160, 60, 160)
