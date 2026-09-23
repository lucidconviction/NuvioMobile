"""Teamskeet Thickumz scraper — collects movie page links from series listing.

Note: Teamskeet uses JavaScript rendering; direct MP4 URLs are not available
in static HTML. This scraper collects movie page metadata for reference.

Site: https://www.teamskeet.com/series/thickumz
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "Teamskeet-Thickumz"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_teamskeet_thickumz():
    results = []
    html = fetch_with_timeout(
        "https://www.teamskeet.com/series/thickumz",
        timeout_ms=15000,
        headers={"User-Agent": UA},
    )
    if not html:
        return results

    links = re.findall(
        r'"(https?://www\.teamskeet\.com/movies/[^"]+)"',
        html,
    )
    links = sorted(set(links))[:MAX_VIDEOS]

    streams = []
    for url in links:
        parts = url.rstrip("/").split("/")
        slug = parts[-1] if parts else "movie"
        title = slug.replace("-", " ").title()
        s = create_stream(title, url, GROUP_TITLE)
        s.referrer = "https://www.teamskeet.com/series/thickumz"
        s.user_agent = UA
        streams.append(s)

    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_teamskeet_thickumz():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
