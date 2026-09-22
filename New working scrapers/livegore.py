"""LiveGore scraper — ported from NettSmuttProvider.js (scrapeLiveGore).

Collects post links from the listing pages (main + /?start=N + mirror domains)
and resolves each post page's direct .mp4 source.
"""
import os
import re
import sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "LiveGore"
MAX_VIDEOS = 100
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def _extract_post_links(html):
    links = []
    seen = set()

    def add(url):
        url = (url or "").strip().split("?")[0].split("#")[0]
        if url and url.startswith("http") and url not in seen:
            seen.add(url)
            links.append(url)

    # Direct post links
    for m in re.finditer(r'<a[^>]*href="(https://www\.livegore\.com/\d+[^"]*)"[^>]*>', html, re.I):
        add(m.group(1))
    # Post-card class anchors (href is the URL, not the leading attributes)
    for m in re.finditer(r'<a\s+class="[^"]*post[^"]*"[^>]*href="([^"]+)"', html, re.I):
        add(m.group(1))
    # data-link attributes
    for m in re.finditer(r'data-link="([^"]+)"', html, re.I):
        add(m.group(1))
    return links


def _resolve_video(video_url):
    html = fetch_with_timeout(video_url, 15000, {"User-Agent": UA})
    if not html:
        return None
    patterns = [
        r'<source[^>]+src="([^"]+\.mp4[^"]*)"',
        r'<video[^>]*src="([^"]+\.mp4[^"]*)"',
        r'src=["\']([^"\']+\.mp4[^"\']*)["\']',
        r'data-src="([^"]+\.mp4[^"]*)"',
    ]
    src = None
    for pat in patterns:
        m = re.search(pat, html, re.I)
        if m:
            src = m.group(1).split("?")[0]
            break
    if not src:
        return None
    title = "Untitled"
    t = (re.search(r'<meta[^>]+property="og:title"[^>]+content="([^"]+)"', html, re.I)
         or re.search(r"<title>([^<]*)</title>", html, re.I))
    if t:
        title = t.group(1).strip()
    return {"title": title, "mp4": src}


def scrape_livegore():
    results = []
    all_links = []
    seen = set()

    pages = ["https://www.livegore.com/"]
    for i in range(1, 5):
        pages.append(f"https://www.livegore.com/?start={i * 20}")

    for page_url in pages:
        html = fetch_with_timeout(page_url, 15000, {"User-Agent": UA})
        if not html:
            continue
        for l in _extract_post_links(html):
            if l not in seen:
                seen.add(l)
                all_links.append(l)

    if not all_links:
        for alt in ["https://livegore.net/", "https://livegore.org/"]:
            html = fetch_with_timeout(alt, 15000, {"User-Agent": UA})
            if not html:
                continue
            for l in _extract_post_links(html):
                if l not in seen:
                    seen.add(l)
                    all_links.append(l)

    resolved = {}
    resolved_urls = set()
    batch_size = 10
    for i in range(0, len(all_links) and min(len(all_links), MAX_VIDEOS * 4) or 0, batch_size):
        batch = all_links[i:i + batch_size]
        with ThreadPoolExecutor(max_workers=batch_size) as ex:
            for r in ex.map(_resolve_video, batch):
                if r and r["mp4"] not in resolved_urls:
                    resolved_urls.add(r["mp4"])
                    resolved[r["mp4"]] = r

    streams = []
    for v in list(resolved.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = "https://www.livegore.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_livegore():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)