"""Tests for summarize_report.py against the canned dumps in tools/fixtures/report/."""
import contextlib
import io
import os
import shutil
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.dont_write_bytecode = True
sys.path.insert(0, HERE)

import summarize_report as sr  # noqa: E402

REPORT = os.path.join(HERE, "fixtures", "report")


def read(name):
    return sr.read(os.path.join(REPORT, name))


class Parsers(unittest.TestCase):
    def test_frame_times_skip_flagged_frames(self):
        times = sr.frame_times_ms(read("good-dock-framestats.txt"))
        self.assertEqual(len(times), 100)  # the flagged 90 ms frame is dropped
        self.assertEqual(sr.percentile(times, 50), 5.0)
        self.assertEqual(sr.percentile(times, 99), 12.0)

    def test_percentile_edges(self):
        self.assertIsNone(sr.percentile([], 50))
        self.assertEqual(sr.percentile([3.0], 99), 3.0)
        self.assertEqual(sr.percentile([1.0, 2.0, 3.0, 4.0], 50), 2.0)

    def test_janky_pct(self):
        self.assertEqual(sr.janky_pct(read("bad-grid-gfxinfo.txt")), 6.0)

    def test_pss(self):
        self.assertAlmostEqual(sr.pss_mb(read("good-meminfo.txt")), 60.0)
        self.assertAlmostEqual(sr.pss_mb("  TOTAL    10240    1 2"), 10.0)
        self.assertIsNone(sr.pss_mb("nothing here"))

    def test_idle_cpu_takes_last_top_sample(self):
        self.assertEqual(sr.idle_cpu_pct(read("good-idle-cpu.txt")), 0.3)
        self.assertEqual(sr.idle_cpu_pct("  0.7% 1234/com.kevin.glasslauncher: 0.5% user + 0.2% kernel"), 0.7)
        self.assertIsNone(sr.idle_cpu_pct(""))


class Summary(unittest.TestCase):
    def test_within_budget(self):
        text, misses = sr.summarize(REPORT, "good")
        self.assertEqual(misses, 0, text)
        self.assertIn("dock: frame time p50 | 5.0 ms | < 8 ms | PASS", text)
        self.assertIn("Idle RAM (PSS) | 60.0 MB | < 70 MB | PASS", text)

    def test_budget_miss_is_reported(self):
        text, misses = sr.summarize(REPORT, "bad")
        self.assertEqual(misses, 2, text)  # p99 and janky; p50 7 ms is fine
        self.assertIn("grid: janky frames | 6.00% | < 1% | FAIL", text)

    def test_cli_writes_summary_and_exit_code(self):
        with tempfile.TemporaryDirectory() as d:
            for f in os.listdir(REPORT):
                shutil.copy(os.path.join(REPORT, f), d)
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(sr.main(["x", d, "bad"]), 1)
                self.assertTrue(os.path.exists(os.path.join(d, "summary.md")))
                self.assertEqual(sr.main(["x", d, "good"]), 0)


if __name__ == "__main__":
    unittest.main()
