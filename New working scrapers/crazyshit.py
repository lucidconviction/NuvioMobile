"""Crazyshit scraper — ported from NettSmuttProvider.js (scrapeCrazyshit).

Video listing → /cnt/medias/<id>-slug pages → tokenized mediav mp4 source.
Tokens expire (~1h) but the cache keeps them fresh; referer required.
"""
import os
import re
import sys
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "Crazyshit"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

HTML_ENTITIES = {
    "&amp;": "&", "&lt;": "<", "&gt;": ">", "&quot;": '"', "&#39;": "'",
    "&apos;": "'", "&nbsp;": " ", "&mdash;": "—", "&ndash;": "–", "&hellip;": "…",
}


def _decode_html(s):
    def repl(m):
        return HTML_ENTITIES.get(m.group(0), m.group(0))
    out = re.sub(r"&(amp|lt|gt|quot|#39|apos|nbsp|mdash|ndash|hellip);", repl, s, flags=re.I)

    def num_repl(m):
        try:
            return chr(int(m.group(0)[2:-1], 10))
        except Exception:
            return m.group(0)
    return re.sub(r"&#\d+;", num_repl, out)


def _extract_media_links(html):
    return list({m.group(1) for m in re.finditer(r'href="(https?://crazyshit\.com/cnt/medias/\d+[^"]*)"', html, re.I)})


def _resolve_media(media_url):
    html = fetch_with_timeout(media_url, 15000, {"User-Agent": UA})
    if not html:
        return None
    src = re.search(r'src=["\']([^"\']+\.mp4[^"\']*)["\']', html, re.I)
    if not src:
        return None
    title = ""
    t = (re.search(r'property="og:title"[^>]+content="([^"]+)"', html, re.I)
         or re.search(r"<title>([^<]*)</title>", html, re.I))
    if t:
        title = _decode_html(re.sub(r"<[^>]+>", " ", t.group(1))).replace("  ", " ").strip()
    return {"url": src.group(1), "title": title or "Crazyshit Video"}


def scrape_crazyshit():
    results = []
    media_links = []
    seen_link = set()

    pages = ["https://crazyshit.com/videos/"] + [f"https://crazyshit.com/videos/{i}/" for i in range(1, 5)]
    for p in pages:
        if len(media_links) >= MAX_VIDEOS * 4:
            break
        html = fetch_with_timeout(p, 15000, {"User-Agent": UA})
        if not html:
            continue
        for l in _extract_media_links(html):
            if l not in seen_link:
                seen_link.add(l)
                media_links.append(l)

    resolved = {}
    resolved_urls = set()
    batch_size = 6
    for i in range(0, len(media_links), batch_size):
        batch = media_links[i:i + batch_size]
        with ThreadPoolExecutor(max_workers=batch_size) as ex:
            for r in ex.map(_resolve_media, batch):
                if r and r["url"] and r["url"] not in resolved_urls:
                    resolved_urls.add(r["url"])
                    resolved[r["url"]] = r
        if len(resolved) >= MAX_VIDEOS:
            break

    streams = []
    for v in list(resolved.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["url"], GROUP_TITLE)
        s.referrer = "https://crazyshit.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_crazyshit():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)