#!/usr/bin/env python3
"""Build Nett_Smutt_3.0.m3u — runs all NettSmutt-group scrapers and emits M3U.

Scrapers (in order):
  crazyshit, usacrime, kaotic, livegore, theync, worldstar
"""
import os
import sys

# Ensure the scrapers directory itself is on sys.path (core.py lives here)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)

from crazyshit import scrape_crazyshit
from usacrime import scrape_usacrime
from kaotic import scrape_kaotic
from livegore import scrape_livegore
from theync import scrape_theync
from worldstar import scrape_worldstar

SCRAPERS = [
    ("crazyshit", scrape_crazyshit),
    ("usacrime", scrape_usacrime),
    ("kaotic", scrape_kaotic),
    ("livegore", scrape_livegore),
    ("theync", scrape_theync),
    ("worldstar", scrape_worldstar),
]


def build():
    lines = ["#EXTM3U"]
    total = 0
    for name, fn in SCRAPERS:
        try:
            results = fn()
        except Exception as e:
            print(f"[WARN] {name} scraper failed: {e}", file=sys.stderr)
            continue
        for group in results:
            for s in group.get("streams", []):
                title = s.title.replace(",", " ")
                group_title = s.group_title
                lines.append(f'#EXTINF:-1 group-title="{group_title}",{title}')
                lines.append(s.url)
                total += 1
    print(f"# Total streams: {total}", file=sys.stderr)
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    out = build()
    sys.stdout.write(out)