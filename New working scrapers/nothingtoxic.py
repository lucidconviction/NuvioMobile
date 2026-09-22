"""NothingToxic scraper — extracts video links and titles from listing pages."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "NothingToxic"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_nothingtoxic():
    results = []
    all_videos = {}
    for page in range(1, 10):
        if len(all_videos) >= MAX_VIDEOS:
            break
        url = "https://nothingtoxic.com/" if page == 1 else f"https://nothingtoxic.com/page/{page}/"
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue

        for m in re.finditer(r'href="(https?://nothingtoxic\.com/out/\d+/)"[^>]*title="([^"]+)"', html):
            out_url = m.group(1)
            title = m.group(2)
            if out_url in all_videos:
                continue
            all_videos[out_url] = {"title": title, "url": out_url}
            if len(all_videos) >= MAX_VIDEOS:
                break

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["url"], GROUP_TITLE)
        s.referrer = "https://nothingtoxic.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_nothingtoxic():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
