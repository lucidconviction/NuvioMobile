"""SickJunk scraper — WordPress blog with direct MP4 uploads.

Posts:  https://sickjunk.com/{slug}/
MP4s:   https://sickjunk.com/wp-content/uploads/{year}/{month}/{day}/{Title}.mp4
Also has YouTube embeds (skipped — no direct MP4 from those).
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "SickJunk"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_sickjunk():
    results = []
    post_links = []
    seen = set()

    for page in range(1, 4):
        url = f"https://sickjunk.com/page/{page}/" if page > 1 else "https://sickjunk.com/"
        html = fetch_with_timeout(url, timeout_ms=15000, headers={"User-Agent": UA})
        if not html:
            continue
        for m in re.finditer(r'href="(https?://sickjunk\.com/[^/"][^"]+)"', html):
            link = m.group(1)
            if "/page/" in link or "/feed/" in link:
                continue
            if link not in seen:
                seen.add(link)
                post_links.append(link)
        if len(post_links) >= MAX_VIDEOS * 2:
            break

    resolved = {}
    resolved_urls = set()

    for link in post_links:
        if len(resolved) >= MAX_VIDEOS:
            break
        html = fetch_with_timeout(link, timeout_ms=15000, headers={"User-Agent": UA})
        if not html:
            continue
        mp4 = re.search(r'(https?://sickjunk\.com/wp-content/uploads/[^\s"\'<>]+\.[Mm][Pp]4)', html)
        if not mp4:
            continue
        url = mp4.group(1)
        if url in resolved_urls:
            continue
        resolved_urls.add(url)
        title = "SickJunk Video"
        t = re.search(r'<title>([^<]+)</title>', html, re.I)
        if t:
            title = t.group(1).strip().replace(" - SickJunk.com", "").replace(" - Humanity At Its Worst", "").strip()
        resolved[url] = {"title": title, "url": url}

    streams = []
    for v in list(resolved.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["url"], GROUP_TITLE)
        s.referrer = "https://sickjunk.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_sickjunk():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
