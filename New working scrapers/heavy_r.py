"""Heavy-R scraper — uses RSS feed for video links, resolves each page for direct MP4.

Video pages: https://www.heavy-r.com/video/{id}/{slug}/
MP4 source: <source src="https://a-cdn.heavy-r.com/vid/...">
"""
import os
import re
import sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "Heavy-R"
MAX_VIDEOS = 100
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_heavy_r():
    results = []
    rss_html = fetch_with_timeout("https://www.heavy-r.com/modules/rss.php?type=recent", timeout_ms=15000, headers={"User-Agent": UA})
    if not rss_html:
        return results

    video_links = re.findall(r'<link>(https?://www\.heavy-r\.com/video/\d+/[^<]+)</link>', rss_html)
    video_links = list(dict.fromkeys(video_links))[:MAX_VIDEOS * 2]

    resolved = {}
    resolved_urls = set()

    def resolve_video(link):
        html = fetch_with_timeout(link, timeout_ms=15000, headers={"User-Agent": UA})
        if not html:
            return None
        m = re.search(r'<source[^>]+src=["\']([^"\']+\.mp4[^"\']*)["\']', html, re.I)
        if not m:
            return None
        url = m.group(1)
        if url in resolved_urls:
            return None
        title = "Heavy-R Video"
        t = re.search(r'<title>([^<]+)</title>', html, re.I)
        if t:
            title = t.group(1).strip()
        return {"url": url, "title": title}

    batch_size = 10
    for i in range(0, len(video_links), batch_size):
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
        s.referrer = "https://www.heavy-r.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_heavy_r():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
