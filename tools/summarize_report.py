#!/usr/bin/env python3
"""Turn the files written by tools/measure.sh into summary.md, judged against the spec budgets.

Usage: tools/summarize_report.py <report-dir> <name>
Reads <name>-<phase>-gfxinfo.txt / -framestats.txt, <name>-meminfo.txt, <name>-idle-cpu.txt.
Writes <report-dir>/summary.md and prints it; exits 1 when a budget is missed.
"""
import glob
import math
import os
import re
import sys

sys.dont_write_bytecode = True

P50_BUDGET_MS = 8.0
P99_BUDGET_MS = 16.7
JANK_BUDGET_PCT = 1.0
PSS_BUDGET_MB = 70.0
IDLE_CPU_BUDGET_PCT = 1.0  # "~0%": anything under one percent of a core counts as idle


def percentile(sorted_values, pct):
    """Nearest-rank percentile of an ascending list."""
    if not sorted_values:
        return None
    rank = max(1, math.ceil(pct / 100 * len(sorted_values)))
    return sorted_values[rank - 1]


def frame_times_ms(framestats_text):
    """Per-frame time (IntendedVsync -> FrameCompleted) from `dumpsys gfxinfo <pkg> framestats`."""
    times = []
    in_block, columns = False, None
    for line in framestats_text.splitlines():
        line = line.strip()
        if line == "---PROFILEDATA---":
            in_block, columns = not in_block, None
            continue
        if not in_block or not line:
            continue
        cells = line.rstrip(",").split(",")
        if columns is None:
            columns = {name: i for i, name in enumerate(cells)}
            continue
        try:
            if int(cells[columns["Flags"]]) != 0:  # flagged frames (layout pass, skipped) are not steady-state
                continue
            start, end = int(cells[columns["IntendedVsync"]]), int(cells[columns["FrameCompleted"]])
        except (KeyError, ValueError, IndexError):
            continue
        if end > start:
            times.append((end - start) / 1e6)
    return sorted(times)


def gfxinfo_value(text, label):
    m = re.search(rf"^\s*{re.escape(label)}:\s*([\d.]+)", text, re.MULTILINE)
    return float(m.group(1)) if m else None


def janky_pct(gfxinfo_text):
    m = re.search(r"^\s*Janky frames:\s*\d+\s*\(([\d.]+)%\)", gfxinfo_text, re.MULTILINE)
    return float(m.group(1)) if m else None


def pss_mb(meminfo_text):
    m = re.search(r"TOTAL PSS:\s*(\d+)", meminfo_text) or re.search(r"^\s*TOTAL\s+(\d+)", meminfo_text, re.MULTILINE)
    return int(m.group(1)) / 1024 if m else None


def idle_cpu_pct(cpu_text):
    """Last sample from toybox `top -o PID,%CPU,...` or a `dumpsys cpuinfo` line ('0.3% 123/pkg: ...')."""
    value = None
    for line in cpu_text.splitlines():
        top = re.match(r"^\s*\d+\s+([\d.]+)\s+\S+\s+\S+", line)
        info = re.match(r"^\s*([\d.]+)%\s+\d+/", line)
        if top or info:
            value = float((top or info).group(1))
    return value


def verdict(value, limit):
    return "n/a" if value is None else ("PASS" if value < limit else "FAIL")


def fmt(value, unit, digits=1):
    return "n/a" if value is None else f"{value:.{digits}f}{unit}"


def read(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        return f.read()


def summarize(report_dir, name):
    lines = [f"# Measurements: {name}", "", "| Metric | Measured | Budget | Result |", "| --- | --- | --- | --- |"]
    misses = 0

    def row(label, value, unit, limit, digits=1):
        nonlocal misses
        result = verdict(value, limit)
        misses += result == "FAIL"
        lines.append(f"| {label} | {fmt(value, unit, digits)} | < {limit:g}{unit} | {result} |")

    phases = sorted(os.path.basename(p) for p in glob.glob(os.path.join(report_dir, f"{name}-*-framestats.txt")))
    for stats_file in phases:
        phase = stats_file[len(name) + 1:-len("-framestats.txt")]
        times = frame_times_ms(read(os.path.join(report_dir, stats_file)))
        info_path = os.path.join(report_dir, f"{name}-{phase}-gfxinfo.txt")
        jank = janky_pct(read(info_path)) if os.path.exists(info_path) else None
        lines.append(f"| {phase}: frames measured | {len(times)} | | |")
        row(f"{phase}: frame time p50", percentile(times, 50), " ms", P50_BUDGET_MS)
        row(f"{phase}: frame time p99", percentile(times, 99), " ms", P99_BUDGET_MS)
        row(f"{phase}: janky frames", jank, "%", JANK_BUDGET_PCT, 2)

    mem_path = os.path.join(report_dir, f"{name}-meminfo.txt")
    row("Idle RAM (PSS)", pss_mb(read(mem_path)) if os.path.exists(mem_path) else None, " MB", PSS_BUDGET_MB)
    cpu_path = os.path.join(report_dir, f"{name}-idle-cpu.txt")
    row("Idle CPU", idle_cpu_pct(read(cpu_path)) if os.path.exists(cpu_path) else None, "%", IDLE_CPU_BUDGET_PCT)

    lines += ["", "Frame times come from framestats (IntendedVsync to FrameCompleted); janky % is gfxinfo's own count.",
              "Budgets: spec 2026-10-17-glass-launcher-design.md, 'Performance and feel'.", ""]
    return "\n".join(lines), misses


def main(argv):
    if len(argv) != 3:
        print(__doc__, file=sys.stderr)
        return 2
    text, misses = summarize(argv[1], argv[2])
    with open(os.path.join(argv[1], "summary.md"), "w", encoding="utf-8") as f:
        f.write(text)
    print(text)
    return 1 if misses else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
