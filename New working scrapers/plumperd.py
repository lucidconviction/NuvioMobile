"""PlumperD scraper — extracts low-res preview clips from listing page.

Site: https://plumperd.com/
Previews: https://cdnstatic.imctransfer.com/static_01/.../preview_320.mp4
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "PlumperD"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_plumperd():
    results = []
    html = fetch_with_timeout("https://plumperd.com/", timeout_ms=15000, headers={"User-Agent": UA})
    if not html:
        return results

    mp4s = re.findall(
        r"(https?://cdnstatic\.imctransfer\.com/[^\s\"'<>]+preview_320\.mp4)",
        html,
        re.I,
    )
    mp4s = sorted(set(mp4s))[:MAX_VIDEOS]

    streams = []
    for url in mp4s:
        # Extract numeric ID from URL path
        parts = url.split("/")
        vid_id = parts[-2] if len(parts) >= 2 else ""
        title = f"PlumperD Preview {vid_id}"
        s = create_stream(title, url, GROUP_TITLE)
        s.referrer = "https://plumperd.com/"
        s.user_agent = UA
        streams.append(s)

    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_plumperd():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
