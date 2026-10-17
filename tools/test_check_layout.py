"""Tests for check_layout.py against the hand-written dumps in tools/fixtures/. Run: python3 tools/test_check_layout.py"""
import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.dont_write_bytecode = True
sys.path.insert(0, HERE)

from layout_checks import FAIL, PASS, Params, run_checks  # noqa: E402
from layout_model import GRID_TILE, Layout, Rect, parse_dump  # noqa: E402

FIXTURES = os.path.join(HERE, "fixtures")
PARAMS = Params(scale=1.15, margin_px=12.0)  # 6dp at density 2


def failed_checks(name: str) -> set:
    layout = parse_dump(os.path.join(FIXTURES, f"layout_{name}.xml"))
    return {f.name for f in run_checks(layout, PARAMS) if f.status == FAIL}


class LayoutChecks(unittest.TestCase):
    def test_pass(self):
        layout = parse_dump(os.path.join(FIXTURES, "layout_pass.xml"))
        findings = run_checks(layout, PARAMS)
        self.assertEqual([f for f in findings if f.status == FAIL], [])
        self.assertGreaterEqual(len([f for f in findings if f.status == PASS]), 10)

    def test_overlap(self):
        self.assertIn("no tile overlaps another", failed_checks("overlap"))

    def test_misaligned(self):
        self.assertIn("dock tile columns align with grid columns", failed_checks("misaligned"))

    def test_uneven_gap(self):
        self.assertIn("grid_tile gaps equal", failed_checks("uneven_gap"))

    def test_slab_too_tight(self):
        self.assertEqual(failed_checks("tight_slab"), {"dock slab holds scaled tiles + margin"})

    def test_gap_too_small_for_scale(self):
        layout = parse_dump(os.path.join(FIXTURES, "layout_pass.xml"))
        findings = run_checks(layout, Params(scale=1.5, margin_px=12.0))
        self.assertIn("dock_tile gap fits focus scale", {f.name for f in findings if f.status == FAIL})

    def test_one_focused_tile_needs_half_its_growth_plus_margin(self):
        # 248px tiles at 1.15x grow 18.6px a side; + 12px margin = 31px. Two such neighbours 32px apart pass.
        tiles = [Rect(100, 300, 348, 440), Rect(380, 300, 628, 440)]
        layout = Layout(Rect(0, 0, 1920, 1080), {GRID_TILE: tiles})
        gap = [f for f in run_checks(layout, PARAMS) if f.name == "grid_tile gap fits focus scale"]
        self.assertEqual(gap[0].status, PASS, gap[0].detail)
        tiles[1] = Rect(377, 300, 625, 440)  # 29px apart
        gap = [f for f in run_checks(layout, PARAMS) if f.name == "grid_tile gap fits focus scale"]
        self.assertEqual(gap[0].status, FAIL)

    def test_rows_cut_off_by_the_screen_edge_are_ignored(self):
        tiles = [Rect(100, 800, 348, 940), Rect(380, 800, 628, 940), Rect(100, 990, 348, 1080)]
        layout = Layout(Rect(0, 0, 1920, 1080), {GRID_TILE: tiles})
        sizes = [f for f in run_checks(layout, PARAMS) if f.name == "grid_tile sizes equal"]
        self.assertEqual(sizes[0].status, PASS, sizes[0].detail)

    def test_no_home_screen_is_a_failure(self):
        with tempfile.NamedTemporaryFile("w", suffix=".xml", delete=False) as f:
            f.write('<hierarchy><node bounds="[0,0][1920,1080]" resource-id=""/></hierarchy>')
        try:
            findings = run_checks(parse_dump(f.name), PARAMS)
        finally:
            os.unlink(f.name)
        self.assertEqual([x.status for x in findings], [FAIL])


class Geometry(unittest.TestCase):
    def test_scaled_about_centre(self):
        r = Rect(100, 100, 300, 220).scaled(1.5, margin=10)
        self.assertEqual((r.left, r.top, r.right, r.bottom), (40, 60, 360, 260))

    def test_overlap_area_touching_edges_is_zero(self):
        self.assertEqual(Rect(0, 0, 10, 10).overlap_area(Rect(10, 0, 20, 10)), 0)


class Cli(unittest.TestCase):
    def run_cli(self, name, *extra):
        script = os.path.join(HERE, "check_layout.py")
        return subprocess.run([sys.executable, script, os.path.join(FIXTURES, f"layout_{name}.xml"), "--no-annotate", *extra],
                              capture_output=True, text=True)

    def test_exit_codes(self):
        self.assertEqual(self.run_cli("pass").returncode, 0)
        bad = self.run_cli("overlap")
        self.assertEqual(bad.returncode, 1)
        self.assertIn("FAIL no tile overlaps another", bad.stdout)

    def test_density_flag(self):
        # Unscaled tiles with a 6dp margin: 6px at density 1 fits the slab, 12px at density 2 does not.
        self.assertEqual(self.run_cli("tight_slab", "--scale", "1", "--density", "1").returncode, 0)
        self.assertEqual(self.run_cli("tight_slab", "--scale", "1", "--density", "2").returncode, 1)

    def test_annotated_png_when_pillow_present(self):
        try:
            from PIL import Image
        except ImportError:
            self.skipTest("Pillow not installed")
        with tempfile.TemporaryDirectory() as d:
            xml = os.path.join(d, "home.xml")
            with open(os.path.join(FIXTURES, "layout_pass.xml")) as src, open(xml, "w") as dst:
                dst.write(src.read())
            Image.new("RGB", (1920, 1080), (20, 20, 30)).save(os.path.join(d, "home.png"))
            result = subprocess.run([sys.executable, os.path.join(HERE, "check_layout.py"), xml], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertTrue(os.path.exists(os.path.join(d, "home.annotated.png")))


if __name__ == "__main__":
    unittest.main()
