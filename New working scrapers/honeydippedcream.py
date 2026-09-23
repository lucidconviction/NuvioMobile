"""HoneyDippedCream scraper — extracts direct MP4s from listing page.

Site: https://honeydippedcream.com/
MP4s: https://thumbs.honeydippedcream.yppcdn.com/.../hdcXXX_honedc_TITLE_large.mp4
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "HoneyDippedCream"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def _slug_to_title(slug: str) -> str:
    slug = slug.replace("-", " ")
    slug = re.sub(r"amp", "&", slug, flags=re.I)
    slug = re.sub(r"\bft\b", "ft", slug, flags=re.I)
    return slug.strip().title()


def scrape_honeydippedcream():
    results = []
    html = fetch_with_timeout("https://honeydippedcream.com/", timeout_ms=15000, headers={"User-Agent": UA})
    if not html:
        return results

    mp4s = re.findall(
        r"(https?://[^\s\"'<>]+\.(?:mp4)[^\s\"'<>]*)",
        html,
        re.I,
    )
    unique = sorted(set(u for u in mp4s if "_large.mp4" in u and "trailer" not in u.lower()))

    streams = []
    for url in unique[:MAX_VIDEOS]:
        fname = url.rsplit("/", 1)[-1]
        title = _slug_to_title(fname.replace(".mp4", "").replace("_honedc_", " "))
        s = create_stream(title, url, GROUP_TITLE)
        s.referrer = "https://honeydippedcream.com/"
        s.user_agent = UA
        streams.append(s)

    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_honeydippedcream():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
