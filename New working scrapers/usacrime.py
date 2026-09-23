"""USACrime scraper — ported from NettSmuttProvider.js (scrapeUsaCrime).

Archive listing (home + /page/N) → article cards → slug; the video serves
directly at https://cdn.usacrime.com/<slug>.mp4. Premium-gated cards carry a
.uc-premium-badge and are skipped. Clips are hotlink-protected (need a Referer).
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "USACrime"
MAX_VIDEOS = 100
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

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


def _extract_posts(html):
    posts = []
    for m in re.finditer(r'<li class="entry-list-item">([\s\S]*?)</li>', html, re.I):
        block = m.group(1)
        if "uc-premium-badge" in block:
            continue
        href = (re.search(r'href="https://usacrime\.com/([a-z0-9][a-z0-9-]+)/"', block, re.I)
                or re.search(r'href="//([a-z0-9][a-z0-9-]+)/"', block, re.I))
        if not href:
            continue
        slug = href.group(1)
        title = ""
        t = re.search(r'<h2[^>]*class="entry-title"[^>]*>\s*<a[^>]*>([\s\S]*?)</a>', block, re.I)
        if t:
            title = _decode_html(re.sub(r"<[^>]+>", " ", t.group(1))).replace("  ", " ").strip()
        posts.append({"slug": slug, "title": title or slug.replace("-", " ")})
    return posts


def _resolve_mp4(post_url: str) -> str | None:
    """Fetch the article page and extract the <video src> (hotlink-protected)."""
    html = fetch_with_timeout(post_url, 15000, {"User-Agent": UA})
    if not html:
        return None
    m = re.search(r'<video[^>]+src="([^"]+)"', html, re.I)
    if m:
        return m.group(1)
    m = re.search(r'<source[^>]+src="([^"]+\.mp4[^"]*)"', html, re.I)
    return m.group(1) if m else None


def scrape_usacrime():
    results = []
    seen = {}
    pages = ["https://usacrime.com/"] + [f"https://usacrime.com/page/{i}/" for i in range(2, 8)]

    for url in pages:
        if len(seen) >= MAX_VIDEOS:
            break
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue
        for p in _extract_posts(html):
            if p["slug"] not in seen:
                seen[p["slug"]] = p["title"]

    # Resolve each post to its actual MP4 URL (with Referer auth)
    resolved = {}
    batch = 6
    for i in range(0, len(seen), batch):
        if len(resolved) >= MAX_VIDEOS:
            break
        items = list(seen.items())[i:i + batch]
        for slug, title in items:
            post_url = f"https://usacrime.com/{slug}/"
            mp4 = _resolve_mp4(post_url)
            if mp4:
                resolved[slug] = (title, mp4)

    streams = []
    for slug, (title, mp4) in list(resolved.items())[:MAX_VIDEOS]:
        s = create_stream(title, mp4, GROUP_TITLE)
        s.referrer = "https://usacrime.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_usacrime():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)