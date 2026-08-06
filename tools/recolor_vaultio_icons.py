"""
Build Vaultio adaptive-icon foregrounds from the approved reference:
  - release / nightly / debug dome color variants
  - scaled into the Android adaptive safe zone so circular masks don't clip
"""
from pathlib import Path

import numpy as np
from PIL import Image

SRC = Path(
    r"C:\Users\Hayami\.cursor\projects\c-Users-Hayami-AndroidStudioProjects-Vaultio"
    r"\assets\c__Users_Hayami_AppData_Roaming_Cursor_User_workspaceStorage_"
    r"599ad5a627a75f92097c9c26a1715079_images_image-6998f240-5da8-4dc0-a1de-f28937b942d5.png"
)
OUT = Path(r"C:\Users\Hayami\AndroidStudioProjects\Vaultio\app\src")
ASSETS = Path(
    r"C:\Users\Hayami\.cursor\projects\c-Users-Hayami-AndroidStudioProjects-Vaultio\assets"
)

# Adaptive icon: 108dp canvas; important content stays in the inner 72dp.
# Circle-masked ball diameter = 72/108 so the full round icon is always visible.
ICON_SCALE = 72 / 108  # ≈ 0.667
CANVAS = 1024


def dome_red_mask(arr: np.ndarray) -> np.ndarray:
    r, g, b, a = arr[..., 0], arr[..., 1], arr[..., 2], arr[..., 3]
    return (a > 200) & (r > 140) & (g < 110) & (b < 110) & (r > g + 40) & (r > b + 40)


def shade_to_target(src_rgb: np.ndarray, target: tuple[int, int, int]) -> np.ndarray:
    src = src_rgb.astype(np.float32)
    med = np.maximum(np.median(src, axis=0), 1.0)
    scale = np.array(target, dtype=np.float32) / med
    return np.clip(src * scale, 0, 255).astype(np.uint8)


def variant(
    base: np.ndarray,
    dome_rgb: tuple[int, int, int],
    stripe_rgb: tuple[int, int, int] | None,
    stripe_y0: float = 0.36,
    stripe_y1: float = 0.47,
) -> np.ndarray:
    out = base.copy()
    mask = dome_red_mask(base)
    h = base.shape[0]
    y0, y1 = int(h * stripe_y0), int(h * stripe_y1)
    yy = np.arange(h)[:, None]
    stripe_band = (yy >= y0) & (yy <= y1)

    dome_only = mask & ~stripe_band if stripe_rgb is not None else mask
    if dome_only.any():
        out[dome_only, :3] = shade_to_target(base[dome_only, :3], dome_rgb)

    if stripe_rgb is not None:
        stripe = mask & stripe_band
        if stripe.any():
            out[stripe, :3] = shade_to_target(base[stripe, :3], stripe_rgb)
    return out


def apply_circle_mask(img: Image.Image) -> Image.Image:
    """Clip artwork to a circle so launcher masks never show flat squircle sides."""
    w, h = img.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    img = img.crop((left, top, left + side, top + side))

    yy, xx = np.ogrid[:side, :side]
    cx = cy = (side - 1) / 2.0
    r = side / 2.0 - 0.5
    dist = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    mask_arr = np.zeros((side, side), dtype=np.uint8)
    mask_arr[dist <= r - 1.0] = 255
    rim = (dist > r - 1.0) & (dist <= r + 0.5)
    mask_arr[rim] = np.clip(((r + 0.5) - dist[rim]) / 1.5 * 255, 0, 255).astype(np.uint8)
    mask = Image.fromarray(mask_arr, "L")

    out = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def fit_icon(arr: np.ndarray, scale: float = ICON_SCALE, canvas: int = CANVAS) -> np.ndarray:
    """Circle-mask artwork and center it to fill circular adaptive masks."""
    img = Image.fromarray(arr, "RGBA")
    alpha = arr[..., 3]
    ys, xs = np.where(alpha > 8)
    if len(xs) == 0:
        return np.zeros((canvas, canvas, 4), dtype=np.uint8)
    left, right = int(xs.min()), int(xs.max())
    top, bottom = int(ys.min()), int(ys.max())
    cropped = img.crop((left, top, right + 1, bottom + 1))
    circled = apply_circle_mask(cropped)

    target = max(1, int(round(canvas * scale)))
    circled = circled.resize((target, target), Image.Resampling.LANCZOS)

    out = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    ox = (canvas - target) // 2
    oy = (canvas - target) // 2
    out.paste(circled, (ox, oy), circled)
    return np.array(out)


def save_webp(arr: np.ndarray, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(arr, "RGBA").save(path, "WEBP", quality=95, method=6)
    print("wrote", path, arr.shape)


def main() -> None:
    base = np.array(Image.open(SRC).convert("RGBA"))
    print("source", base.shape, "red pixels", int(dome_red_mask(base).sum()))

    variants = {
        "main": base.copy(),
        "nightly": variant(base, dome_rgb=(43, 108, 176), stripe_rgb=(211, 47, 47)),
        "debug": variant(base, dome_rgb=(26, 26, 30), stripe_rgb=(249, 199, 79)),
    }

    for flavor, art in variants.items():
        padded = fit_icon(art)
        save_webp(padded, OUT / f"{flavor}/res/drawable/vaultio_icon.webp")
        Image.fromarray(padded).save(ASSETS / f"vaultio-safe-{flavor}.png")

    print(f"icon scale={ICON_SCALE:.0%} (circle-masked) on {CANVAS}px canvas")


if __name__ == "__main__":
    main()
