"""Parse a uiautomator dump into the rectangles check_layout.py cares about (stdlib only)."""
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass

BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")

DOCK = "dock"
DOCK_TILE = "dock_tile"
GRID_TILE = "grid_tile"
SHELF_TILE = "shelf_tile"
STATUS_PILL = "status_pill"
TILE_KINDS = (DOCK_TILE, GRID_TILE, SHELF_TILE)
KINDS = (DOCK, DOCK_TILE, GRID_TILE, SHELF_TILE, STATUS_PILL)


@dataclass(frozen=True)
class Rect:
    left: int
    top: int
    right: int
    bottom: int

    @property
    def width(self) -> int:
        return self.right - self.left

    @property
    def height(self) -> int:
        return self.bottom - self.top

    @property
    def cx(self) -> float:
        return (self.left + self.right) / 2

    @property
    def cy(self) -> float:
        return (self.top + self.bottom) / 2

    def scaled(self, factor: float, margin: float = 0.0) -> "Rect":
        """This rect scaled about its centre by `factor`, then grown by `margin` on every side."""
        half_w = self.width * factor / 2 + margin
        half_h = self.height * factor / 2 + margin
        return Rect(self.cx - half_w, self.cy - half_h, self.cx + half_w, self.cy + half_h)

    def overlap_area(self, other: "Rect") -> float:
        w = min(self.right, other.right) - max(self.left, other.left)
        h = min(self.bottom, other.bottom) - max(self.top, other.top)
        return w * h if w > 0 and h > 0 else 0

    def overflow_inside(self, outer: "Rect") -> float:
        """Largest distance this rect sticks out of `outer` (0 when fully inside)."""
        return max(0, outer.left - self.left, outer.top - self.top, self.right - outer.right, self.bottom - outer.bottom)


@dataclass(frozen=True)
class Layout:
    screen: Rect
    nodes: dict  # kind -> list[Rect], in document order

    def of(self, kind: str) -> list:
        return self.nodes.get(kind, [])


def resource_kind(resource_id: str):
    """Compose test tags appear as the bare tag; accept `pkg:id/tag` too."""
    tail = resource_id.rsplit("/", 1)[-1]
    return tail if tail in KINDS else None


def parse_rect(text: str):
    m = BOUNDS.fullmatch(text.strip())
    return Rect(*map(int, m.groups())) if m else None


def parse_dump(path: str) -> Layout:
    root = ET.parse(path).getroot()
    nodes = {kind: [] for kind in KINDS}
    screen = None
    for el in root.iter("node"):
        rect = parse_rect(el.get("bounds", ""))
        if rect is None:
            continue
        if screen is None:
            screen = rect
        kind = resource_kind(el.get("resource-id", ""))
        if kind:
            nodes[kind].append(rect)
    if screen is None:
        raise ValueError(f"{path}: no <node bounds=...> found; is this a uiautomator dump?")
    return Layout(screen, nodes)
