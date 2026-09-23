"""Sexy and Funny scraper — WordPress site with video posts containing direct MP4s.

Video pages:  https://sexyandfunny.com/video/{slug}/
MP4s:         https://sexyandfunny.com/wp-content/uploads/{year}/{month}/{slug}.mp4
"""
import html
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "SexyAndFunny"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_sexyandfunny():
    results = []
    video_links = []
    seen = set()

    for page in range(1, 6):
        url = f"https://sexyandfunny.com/video/page/{page}/" if page > 1 else "https://sexyandfunny.com/videos/"
        html_content = fetch_with_timeout(url, timeout_ms=15000, headers={"User-Agent": UA})
        if not html_content:
            continue
        for m in re.finditer(r'href="(https?://sexyandfunny\.com/video/[^\"]+)"', html_content):
            link = m.group(1).rstrip("/")
            if "/page/" in link or "/feed/" in link:
                continue
            if link not in seen:
                seen.add(link)
                video_links.append(link)
        if len(video_links) >= MAX_VIDEOS * 2:
            break

    resolved = {}
    resolved_urls = set()

    for link in video_links:
        if len(resolved) >= MAX_VIDEOS:
            break
        page_html = fetch_with_timeout(link, timeout_ms=15000, headers={"User-Agent": UA})
        if not page_html:
            continue
        mp4 = None
        video_match = re.search(r'<video[^>]*>(.*?)</video>', page_html, re.S)
        if video_match:
            src_match = re.search(r'<source[^>]+src=["\']([^"\']+)["\']', video_match.group(1), re.I)
            if src_match:
                mp4 = src_match.group(1)
        if not mp4:
            for m in re.finditer(r'(https?://sexyandfunny\.com/wp-content/uploads/[^\s"\'<>]+\.[Mm][Pp]4)', page_html):
                url = m.group(1)
                if "banner" not in url and "sfam/banners" not in url:
                    mp4 = url
                    break
        if not mp4:
            continue
        if mp4 in resolved_urls:
            continue
        resolved_urls.add(mp4)
        title = "SexyAndFunny Video"
        t = re.search(r'<title>([^<]+)</title>', page_html, re.I)
        if t:
            title = html.unescape(t.group(1).strip())
            title = re.sub(r'\s*[-–]\s*(Sexy and Funny|Hot Models.*|Free Videos.*)', '', title).strip()
        resolved[mp4] = {"title": title, "url": mp4}

    streams = []
    for v in list(resolved.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["url"], GROUP_TITLE)
        s.referrer = "https://sexyandfunny.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_sexyandfunny():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
