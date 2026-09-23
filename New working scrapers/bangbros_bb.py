"""BangBros Brown Bunnies scraper — collects video page links from listing.

Note: BangBros uses JavaScript rendering; direct MP4 URLs are not available
in static HTML. This scraper collects video page metadata for reference.

Site: https://www.bangbros.com/websites/BrownBunnies
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "BangBros-BrownBunnies"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_bangbros_brownbunnies():
    results = []
    html = fetch_with_timeout(
        "https://www.bangbros.com/websites/BrownBunnies",
        timeout_ms=15000,
        headers={"User-Agent": UA},
    )
    if not html:
        return results

    links = re.findall(
        r'href="(/video/\d+/[^"]+)"',
        html,
    )
    links = sorted(set(links))[:MAX_VIDEOS]

    streams = []
    for path in links:
        full_url = f"https://www.bangbros.com{path}"
        # Derive title from path slug
        parts = path.rstrip("/").split("/")
        slug = parts[-1] if parts else "video"
        title = slug.replace("-", " ").title()
        s = create_stream(title, full_url, GROUP_TITLE)
        s.referrer = "https://www.bangbros.com/websites/BrownBunnies"
        s.user_agent = UA
        streams.append(s)

    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_bangbros_brownbunnies():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
