"""Geometry checks for the Glass Launcher home screen. Pure functions over layout_model.Layout."""
from dataclasses import dataclass
from itertools import combinations
from statistics import median

from layout_model import DOCK, DOCK_TILE, GRID_TILE, SHELF_TILE, STATUS_PILL, TILE_KINDS, Layout, Rect

PASS, FAIL, SKIP = "PASS", "FAIL", "SKIP"
TOLERANCE_PX = 1


@dataclass(frozen=True)
class Finding:
    status: str
    name: str
    detail: str = ""


@dataclass(frozen=True)
class Params:
    scale: float = 1.15
    margin_px: float = 12.0


def _spread(values) -> float:
    return max(values) - min(values)


def _whole(layout: Layout, kind: str) -> list:
    """Tiles of [kind] not cut off by a screen edge (a scrolled grid's partly visible rows)."""
    s = layout.screen
    return [t for t in layout.of(kind) if t.top > s.top and t.bottom < s.bottom and t.left > s.left and t.right < s.right]


def _rows(tiles: list) -> list:
    """Tiles grouped by row (similar top), each row sorted left to right."""
    rows = []
    for tile in sorted(tiles, key=lambda t: (t.top, t.left)):
        if rows and abs(tile.top - rows[-1][0].top) <= max(2, tile.height // 2):
            rows[-1].append(tile)
        else:
            rows.append([tile])
    return [sorted(r, key=lambda t: t.left) for r in rows]


def _horizontal_gaps(rows: list) -> list:
    return [b.left - a.right for row in rows for a, b in zip(row, row[1:])]


def _vertical_gaps(rows: list) -> list:
    return [nxt[0].top - prev[0].bottom for prev, nxt in zip(rows, rows[1:])]


def check_tile_sizes(layout: Layout, _: Params) -> list:
    out = []
    for kind in TILE_KINDS:
        tiles = _whole(layout, kind)
        if len(tiles) < 2:
            out.append(Finding(SKIP, f"{kind} sizes equal", f"{len(tiles)} tile(s)"))
            continue
        dw, dh = _spread([t.width for t in tiles]), _spread([t.height for t in tiles])
        ok = dw <= TOLERANCE_PX and dh <= TOLERANCE_PX
        out.append(Finding(PASS if ok else FAIL, f"{kind} sizes equal", f"{len(tiles)} tiles, width spread {dw}px, height spread {dh}px"))
    return out


def check_equal_gaps(layout: Layout, _: Params) -> list:
    out = []
    for kind in TILE_KINDS:
        rows = _rows(_whole(layout, kind))
        gaps = _horizontal_gaps(rows) + _vertical_gaps(rows)
        if len(gaps) < 2:
            out.append(Finding(SKIP, f"{kind} gaps equal", "fewer than 2 gaps"))
            continue
        spread = _spread(_horizontal_gaps(rows)) if len(_horizontal_gaps(rows)) > 1 else 0
        v_spread = _spread(_vertical_gaps(rows)) if len(_vertical_gaps(rows)) > 1 else 0
        ok = spread <= TOLERANCE_PX and v_spread <= TOLERANCE_PX
        out.append(Finding(PASS if ok else FAIL, f"{kind} gaps equal", f"column gaps {sorted(set(_horizontal_gaps(rows)))}px, row gaps {sorted(set(_vertical_gaps(rows)))}px"))
    return out


def check_dock_matches_grid(layout: Layout, _: Params) -> list:
    dock, grid = layout.of(DOCK_TILE), layout.of(GRID_TILE)
    name = "dock tile columns align with grid columns"
    if not dock or not grid:
        return [Finding(SKIP, name, "needs both dock and grid tiles")]
    centres = [t.cx for t in grid]
    off = [t for t in dock if min(abs(t.cx - c) for c in centres) > TOLERANCE_PX]
    if off:
        return [Finding(FAIL, name, "no grid column under dock tile(s) at x=" + ", ".join(f"{t.cx:.0f}" for t in off))]
    return [Finding(PASS, name, f"{len(dock)} dock tiles")]


def check_gap_fits_focus_scale(layout: Layout, p: Params) -> list:
    out = []
    for kind in (DOCK_TILE, GRID_TILE, SHELF_TILE):
        tiles = _whole(layout, kind)
        rows = _rows(tiles)
        name = f"{kind} gap fits focus scale"
        h_gaps, v_gaps = _horizontal_gaps(rows), _vertical_gaps(rows)
        if not h_gaps and not v_gaps:
            out.append(Finding(SKIP, name, "no neighbours"))
            continue
        # One tile is focused at a time: it grows by half its extra size on each side, and its
        # unscaled neighbour must stay [margin_px] clear of that.
        need_w = median(t.width for t in tiles) * (p.scale - 1) / 2 + p.margin_px
        need_h = median(t.height for t in tiles) * (p.scale - 1) / 2 + p.margin_px
        short = [g for g in h_gaps if g < need_w - TOLERANCE_PX] + [g for g in v_gaps if g < need_h - TOLERANCE_PX]
        smallest = min(h_gaps + v_gaps)
        detail = f"smallest gap {smallest}px, need {need_w:.0f}px across / {need_h:.0f}px down"
        out.append(Finding(FAIL if short else PASS, name, detail))
    return out


def check_dock_slab(layout: Layout, p: Params) -> list:
    name = "dock slab holds scaled tiles + margin"
    slabs, tiles = layout.of(DOCK), layout.of(DOCK_TILE)
    if not slabs or not tiles:
        return [Finding(SKIP, name, "needs dock and dock tiles")]
    slab = slabs[0]
    worst = max(t.scaled(p.scale, p.margin_px).overflow_inside(slab) for t in tiles)
    if worst > TOLERANCE_PX:
        return [Finding(FAIL, name, f"scaled tile sticks out of the slab by {worst:.0f}px")]
    return [Finding(PASS, name, f"{len(tiles)} tiles fit")]


def check_on_screen(layout: Layout, _: Params) -> list:
    out_of = []
    for kind in (DOCK, DOCK_TILE, SHELF_TILE, STATUS_PILL):
        out_of += [(kind, r) for r in layout.of(kind) if r.overflow_inside(layout.screen) > 0]
    # Grid rows scroll, so a row may be cut at the top or bottom edge; columns may not.
    out_of += [(GRID_TILE, r) for r in layout.of(GRID_TILE) if r.left < layout.screen.left or r.right > layout.screen.right]
    name = "nothing off-screen"
    if out_of:
        return [Finding(FAIL, name, "; ".join(f"{k} at [{r.left},{r.top}][{r.right},{r.bottom}]" for k, r in out_of[:4]))]
    return [Finding(PASS, name, f"screen {layout.screen.width}x{layout.screen.height}px")]


def check_no_overlap(layout: Layout, _: Params) -> list:
    tiles = [(k, r) for k in TILE_KINDS for r in layout.of(k)]
    name = "no tile overlaps another"
    hits = [(a, b) for a, b in combinations(tiles, 2) if a[1].overlap_area(b[1]) > 0]
    if hits:
        (ka, ra), (kb, rb) = hits[0]
        return [Finding(FAIL, name, f"{len(hits)} overlap(s), e.g. {ka} [{ra.left},{ra.top}] with {kb} [{rb.left},{rb.top}]")]
    return [Finding(PASS, name, f"{len(tiles)} tiles")]


ALL_CHECKS = (
    check_tile_sizes,
    check_equal_gaps,
    check_dock_matches_grid,
    check_gap_fits_focus_scale,
    check_dock_slab,
    check_on_screen,
    check_no_overlap,
)


def run_checks(layout: Layout, params: Params) -> list:
    if not any(layout.of(k) for k in TILE_KINDS):
        return [Finding(FAIL, "tiles found", "no dock_tile/grid_tile/shelf_tile nodes; not on the home screen, or testTagsAsResourceId is off")]
    return [f for check in ALL_CHECKS for f in check(layout, params)]
