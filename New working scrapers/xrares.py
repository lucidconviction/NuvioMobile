"""Xrares scraper — scrapes video listing pages and resolves each for the stream source.

Video listing: https://www.xrares.com/videos?page=N
Video pages:   https://www.xrares.com/video/{id}/{slug}/
Source:        <source src="https://www.xrares.com/vsrc/h264/{hash}/HD">
"""
import os
import re
import sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "Xrares"
MAX_VIDEOS = 100
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_xrares():
    results = []
    video_links = []
    seen = set()

    for page in range(1, 11):
        url = f"https://www.xrares.com/videos/?page={page}"
        html = fetch_with_timeout(url, timeout_ms=15000, headers={"User-Agent": UA})
        if not html:
            continue
        for m in re.finditer(r'href="(/video/\d+/[^"]+)"', html):
            link = ("https://www.xrares.com" + m.group(1)).rstrip("/")
            if link not in seen:
                seen.add(link)
                video_links.append(link)
        if len(video_links) >= MAX_VIDEOS * 3:
            break

    resolved = {}
    resolved_urls = set()

    def resolve_video(link):
        html = fetch_with_timeout(link, timeout_ms=15000, headers={"User-Agent": UA})
        if not html:
            return None
        m = re.search(r'<source[^>]+src=["\']([^"\']+)["\']', html, re.I)
        if not m:
            return None
        url = m.group(1)
        if url in resolved_urls:
            return None
        title = "Xrares Video"
        t = re.search(r'<title>([^<]+)</title>', html, re.I)
        if t:
            title = t.group(1).strip().replace(" - Xrares", "").replace(" - Free Amateur Porn", "").strip()
        return {"url": url, "title": title}

    batch_size = 10
    for i in range(0, min(len(video_links), MAX_VIDEOS * 3), batch_size):
        batch = video_links[i:i + batch_size]
        with ThreadPoolExecutor(max_workers=batch_size) as ex:
            for r in ex.map(resolve_video, batch):
                if r and r["url"] not in resolved_urls:
                    resolved_urls.add(r["url"])
                    resolved[r["url"]] = r
        if len(resolved) >= MAX_VIDEOS:
            break

    streams = []
    for v in list(resolved.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["url"], GROUP_TITLE)
        s.referrer = "https://www.xrares.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_xrares():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
