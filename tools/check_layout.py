#!/usr/bin/env python3
"""Check the Glass Launcher home layout from a `uiautomator dump`.

Usage: tools/check_layout.py <dump.xml> [--scale 1.15] [--margin-dp 6] [--density auto]
                             [--screenshot <png>] [--no-annotate]

Reads the bounds of the Compose test tags dock, dock_tile, grid_tile, shelf_tile and status_pill
(they appear as resource-id). Prints one PASS/FAIL/SKIP line per check and exits 1 on any FAIL.
With Pillow and a screenshot next to the dump (same name, .png) it also writes
<dump>.annotated.png with the boxes and the scaled dock-tile footprints.
"""
import argparse
import os
import sys

sys.dont_write_bytecode = True  # keep tools/ free of __pycache__
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from layout_annotate import annotate  # noqa: E402
from layout_checks import FAIL, Params, run_checks  # noqa: E402
from layout_model import parse_dump  # noqa: E402

SCREEN_WIDTH_DP = 960


def resolve_density(arg: str, screen_width_px: int) -> float:
    return screen_width_px / SCREEN_WIDTH_DP if arg == "auto" else float(arg)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dump")
    parser.add_argument("--scale", type=float, default=1.15, help="focus scale of a tile (default 1.15)")
    parser.add_argument("--margin-dp", type=float, default=6.0, help="breathing room around a scaled tile")
    parser.add_argument("--density", default="auto", help="px per dp, or auto = screen width / 960dp")
    parser.add_argument("--screenshot", help="PNG to draw on (default: <dump>.png)")
    parser.add_argument("--no-annotate", action="store_true")
    args = parser.parse_args(argv)

    layout = parse_dump(args.dump)
    density = resolve_density(args.density, layout.screen.width)
    params = Params(scale=args.scale, margin_px=args.margin_dp * density)
    print(f"{args.dump}: screen {layout.screen.width}x{layout.screen.height}px, density {density:g}, margin {params.margin_px:g}px")

    findings = run_checks(layout, params)
    for f in findings:
        print(f"{f.status} {f.name}" + (f" - {f.detail}" if f.detail else ""))

    if not args.no_annotate:
        stem = os.path.splitext(args.dump)[0]
        out = stem + ".annotated.png"
        if annotate(layout, params, args.screenshot or stem + ".png", out):
            print(f"annotated: {out}")
        else:
            print("annotated: skipped (Pillow missing, or screenshot absent or unreadable)")
    return 1 if any(f.status == FAIL for f in findings) else 0


if __name__ == "__main__":
    sys.exit(main())
