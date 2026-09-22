#!/usr/bin/env python3
"""Build XXX_2.m3u — runs all XXX-group scrapers and emits M3U.

Scrapers (in order):
  youporn, daftporn, inhumanity, nothingtoxic, zfilmzoriginals, horriblevideos, efukt
"""
import os
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, SCRIPT_DIR)

from youporn import scrape_youporn
from daftporn import scrape_daftporn
from inhumanity import scrape_inhumanity
from nothingtoxic import scrape_nothingtoxic
from zfilmzoriginals import scrape_zfilmzoriginals
from horriblevideos import scrape_horriblevideos
from efukt import scrape_efukt

SCRAPERS = [
    ("youporn", scrape_youporn),
    ("daftporn", scrape_daftporn),
    ("inhumanity", scrape_inhumanity),
    ("nothingtoxic", scrape_nothingtoxic),
    ("zfilmzoriginals", scrape_zfilmzoriginals),
    ("horriblevideos", scrape_horriblevideos),
    ("efukt", scrape_efukt),
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