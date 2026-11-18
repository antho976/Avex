#!/usr/bin/env python3
"""Cold-start timeline for Avex on a connected device.

    tools/perf/coldstart.py                       # 10 cold starts of the perf build
    tools/perf/coldstart.py -n 20 --raw runs.json # keep the raw runs beside a reading
    tools/perf/coldstart.py --package com.quietsoftware.avex   # the installed release

Each run force-stops the app, launches it the way the launcher does, and reads two things:

  * the system's own numbers: `Displayed` (first frame, TTID) and `Fully drawn` (TTFD, Home
    showing real data, reported by the app);
  * the app's `AvexStartup` marks (core/perf/StartupTimeline.kt): what happened in between, each
    in milliseconds since the process was forked.

It prints the median, p95 and worst of every milestone. A milestone missing from some runs is
listed with how many runs had it. Measure the perf build (`./gradlew :app:installPerf`): it is the
release with R8, installed beside the real app, so a run never touches a working install.

Needs only adb and Python 3. Set ANDROID_SERIAL when more than one device is attached.
"""
import argparse
import json
import re
import statistics
import subprocess
import sys
import time

LAUNCHER = "com.forge.app.icon.Default"
MARK = re.compile(r"AvexStartup\s*(?:\(\s*\d+\))?: (\S+) \+(\d+)ms")
DISPLAYED = re.compile(r"Displayed (\S+) for user \d+: \+(?:(\d+)s)?(\d+)ms")
FULLY_DRAWN = re.compile(r"Fully drawn (\S+): \+(?:(\d+)s)?(\d+)ms")


def adb(*args, check=True):
    return subprocess.run(["adb", *args], check=check, capture_output=True, text=True).stdout


def millis(seconds, ms):
    return int(seconds or 0) * 1000 + int(ms)


def one_run(package, settle):
    adb("shell", "am", "force-stop", package)
    time.sleep(1.5)
    adb("logcat", "-c")
    out = adb("shell", "am", "start", "-W", "-n", f"{package}/{LAUNCHER}",
              "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER")
    if "LaunchState: COLD" not in out:
        raise SystemExit(f"not a cold start:\n{out}")
    time.sleep(settle)
    run = {}
    for line in adb("logcat", "-d", "-v", "brief").splitlines():
        if m := MARK.search(line):
            run.setdefault(m.group(1), int(m.group(2)))
        elif (m := DISPLAYED.search(line)) and m.group(1).startswith(package + "/"):
            run["system.displayed"] = millis(m.group(2), m.group(3))
        elif (m := FULLY_DRAWN.search(line)) and m.group(1).startswith(package + "/"):
            run["system.fullyDrawn"] = millis(m.group(2), m.group(3))
    return run


def percentile(values, p):
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, round(p * (len(ordered) - 1)))]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("-n", "--runs", type=int, default=10)
    ap.add_argument("--package", default="com.quietsoftware.avex.perf")
    ap.add_argument("--settle", type=float, default=4.0, help="seconds to wait for Home to finish loading")
    ap.add_argument("--raw", help="write every run's numbers to this JSON file")
    args = ap.parse_args()

    if args.package not in adb("shell", "pm", "list", "packages", args.package):
        raise SystemExit(f"{args.package} is not installed (./gradlew :app:installPerf)")

    runs = []
    for i in range(args.runs):
        run = one_run(args.package, args.settle)
        runs.append(run)
        print(f"run {i + 1:2d}: displayed {run.get('system.displayed', '?')} ms, "
              f"fully drawn {run.get('system.fullyDrawn', '?')} ms", file=sys.stderr)
    adb("shell", "am", "force-stop", args.package)

    if args.raw:
        with open(args.raw, "w") as f:
            json.dump({"package": args.package, "runs": runs}, f, indent=1)

    names = sorted({k for r in runs for k in r},
                   key=lambda k: statistics.median(r[k] for r in runs if k in r))
    print(f"\n{args.package}, {len(runs)} cold starts (ms since process start; system.* since the launch intent)")
    print(f"{'milestone':<22}{'p50':>7}{'p95':>7}{'max':>7}{'runs':>6}")
    for name in names:
        values = [r[name] for r in runs if name in r]
        print(f"{name:<22}{statistics.median(values):>7.0f}{percentile(values, 0.95):>7}{max(values):>7}{len(values):>6}")


if __name__ == "__main__":
    main()
