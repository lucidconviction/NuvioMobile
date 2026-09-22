"""Z-Filmz Originals scraper — extracts trailer URLs from JSON embedded in the page."""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "ZFilmzOriginals"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_zfilmzoriginals():
    results = []
    all_videos = {}
    for page in range(1, 13):
        if len(all_videos) >= MAX_VIDEOS:
            break
        url = f"https://z-filmz-originals.com/videos?page={page}"
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue

        m = re.search(r'<script id="__NEXT_DATA__" type="application/json">(.*?)</script>', html, re.S)
        if not m:
            continue
        try:
            data = json.loads(m.group(1))
            videos = data.get("props", {}).get("pageProps", {}).get("contents", {}).get("data", [])
        except Exception:
            continue

        for v in videos:
            title = v.get("title", "")
            trailer_url = v.get("trailer_url", "")
            if not title or not trailer_url:
                continue
            if trailer_url in all_videos:
                continue
            all_videos[trailer_url] = {"title": title, "mp4": trailer_url}
            if len(all_videos) >= MAX_VIDEOS:
                break

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = "https://z-filmz-originals.com/videos"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_zfilmzoriginals():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
