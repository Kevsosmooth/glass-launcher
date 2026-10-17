"""Draw the measured boxes over a screenshot (needs Pillow; callers degrade gracefully without it)."""
from layout_checks import Params
from layout_model import DOCK, DOCK_TILE, GRID_TILE, KINDS, SHELF_TILE, STATUS_PILL, Layout

COLOURS = {
    DOCK: (255, 255, 255),
    DOCK_TILE: (0, 220, 120),
    GRID_TILE: (0, 160, 255),
    SHELF_TILE: (255, 80, 200),
    STATUS_PILL: (255, 230, 0),
}
FOOTPRINT = (255, 140, 0)


def annotate(layout: Layout, params: Params, screenshot_path: str, out_path: str) -> bool:
    """Write out_path; returns False when Pillow is missing or the screenshot is absent or unreadable."""
    try:
        from PIL import Image, ImageDraw
    except ImportError:
        return False
    try:
        image = Image.open(screenshot_path).convert("RGB")
    except OSError:  # missing or not an image (UnidentifiedImageError is an OSError)
        return False
    sx = image.width / layout.screen.width
    sy = image.height / layout.screen.height
    draw = ImageDraw.Draw(image)

    def box(rect, colour, width):
        draw.rectangle([rect.left * sx, rect.top * sy, rect.right * sx, rect.bottom * sy], outline=colour, width=width)

    for kind in KINDS:
        for rect in layout.of(kind):
            box(rect, COLOURS[kind], 3)
    for rect in layout.of(DOCK_TILE):
        box(rect.scaled(params.scale, params.margin_px), FOOTPRINT, 2)
    image.save(out_path)
    return True
