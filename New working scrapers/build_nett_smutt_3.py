#!/usr/bin/env python3
"""Build Nett_Smutt3.0.m3u — runs all scrapers in the directory and emits M3U.

Scrapers (in order):
  crazyshit, usacrime, kaotic, livegore, theync, worldstar, heavy_r, xrares,
  sickjunk, sexyandfunny, honeydippedcream, jeffsmodels, plumperd,
  bangbros_bb, teamskeet_thickumz, daftporn, efukt, horriblevideos,
  inhumanity, nothingtoxic, youporn, zfilmzoriginals
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
from heavy_r import scrape_heavy_r
from xrares import scrape_xrares
from sickjunk import scrape_sickjunk
from sexyandfunny import scrape_sexyandfunny
from honeydippedcream import scrape_honeydippedcream
from jeffsmodels import scrape_jeffsmodels
from plumperd import scrape_plumperd
from bangbros_bb import scrape_bangbros_brownbunnies
from teamskeet_thickumz import scrape_teamskeet_thickumz
from daftporn import scrape_daftporn
from efukt import scrape_efukt
from horriblevideos import scrape_horriblevideos
from inhumanity import scrape_inhumanity
from nothingtoxic import scrape_nothingtoxic
from youporn import scrape_youporn
from zfilmzoriginals import scrape_zfilmzoriginals

SCRAPERS = [
    ("crazyshit", scrape_crazyshit),
    ("usacrime", scrape_usacrime),
    ("kaotic", scrape_kaotic),
    ("livegore", scrape_livegore),
    ("theync", scrape_theync),
    ("worldstar", scrape_worldstar),
    ("heavy_r", scrape_heavy_r),
    ("xrares", scrape_xrares),
    ("sickjunk", scrape_sickjunk),
    ("sexyandfunny", scrape_sexyandfunny),
    ("honeydippedcream", scrape_honeydippedcream),
    ("jeffsmodels", scrape_jeffsmodels),
    ("plumperd", scrape_plumperd),
    ("bangbros_bb", scrape_bangbros_brownbunnies),
    ("teamskeet_thickumz", scrape_teamskeet_thickumz),
    ("daftporn", scrape_daftporn),
    ("efukt", scrape_efukt),
    ("horriblevideos", scrape_horriblevideos),
    ("inhumanity", scrape_inhumanity),
    ("nothingtoxic", scrape_nothingtoxic),
    ("youporn", scrape_youporn),
    ("zfilmzoriginals", scrape_zfilmzoriginals),
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