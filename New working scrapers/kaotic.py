"""Kaotic scraper — ported from NettSmuttProvider.js (scrapeKaotic).

Recent posts listing → each video post page → direct mp4/webm source.
"""
import os
import re
import sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "Kaotic"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_kaotic():
    results = []
    html = fetch_with_timeout("https://kaotic.com/recent/", 15000, {"User-Agent": UA})
    if not html:
        return results

    post_urls = list({
        m.group(1)
        for m in re.finditer(r'href="(https://kaotic\.com/video/[\w]+)"', html, re.I)
    })

    resolved = {}
    resolved_urls = set()

    def resolve(url):
        page = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not page:
            return
        src = re.search(r'src="([^"]*\.(?:mp4|webm)[^"]*)"', page, re.I)
        if not src or src.group(1) in resolved_urls:
            return
        resolved_urls.add(src.group(1))
        t = re.search(r"<title>([^<]*)</title>", page, re.I)
        resolved[src.group(1)] = t.group(1).strip() if t else "Untitled"

    batch_size = 10
    for i in range(0, len(post_urls), batch_size):
        batch = post_urls[i:i + batch_size]
        with ThreadPoolExecutor(max_workers=batch_size) as ex:
            ex.map(resolve, batch)
        if len(resolved) >= MAX_VIDEOS:
            break

    streams = []
    for url, title in list(resolved.items())[:MAX_VIDEOS]:
        s = create_stream(title, url, GROUP_TITLE)
        s.referrer = "https://kaotic.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_kaotic():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)