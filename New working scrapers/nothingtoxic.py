"""NothingToxic scraper — extracts tokenized MP4s from fleshed.com embed pages.

Site: https://nothingtoxic.com/
Each out page embeds a fleshed.com iframe that serves the actual video.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "NothingToxic"
MAX_VIDEOS = 15
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def _resolve_fleshed_embed(embed_url: str) -> str | None:
    """Fetch the fleshed.com embed page and extract the <source src> MP4."""
    html = fetch_with_timeout(embed_url, 15000, {"User-Agent": UA, "Referer": "https://nothingtoxic.com/"})
    if not html:
        return None
    m = re.search(r'<source[^>]+src="([^"]+)"', html, re.I)
    if m:
        return m.group(1).replace("&amp;", "&")
    return None


def scrape_nothingtoxic():
    results = []
    all_videos = {}
    # Collect all out URLs from first 2 pages
    for page in range(1, 3):
        url = "https://nothingtoxic.com/" if page == 1 else f"https://nothingtoxic.com/page/{page}/"
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue
        for m in re.finditer(r'href="(https?://nothingtoxic\.com/out/\d+/)"[^>]*title="([^"]+)"', html):
            out_url = m.group(1)
            title = m.group(2).replace("&amp;", "&").replace("&#039;", "'")
            if out_url not in all_videos:
                all_videos[out_url] = {"title": title, "out_url": out_url}

    # Resolve each out page to its fleshed.com embed, then to the MP4
    streams = []
    for v in list(all_videos.values())[:30]:  # Limit to 30 pages to resolve
        if len(streams) >= MAX_VIDEOS:
            break
        out_html = fetch_with_timeout(v["out_url"], 15000, {"User-Agent": UA})
        if not out_html:
            continue
        emb = re.search(r'iframe[^>]+src=["\'](https?://fleshed\.com/embed/\d+)["\']', out_html, re.I)
        if not emb:
            continue
        mp4 = _resolve_fleshed_embed(emb.group(1))
        if not mp4:
            continue
        s = create_stream(v["title"], mp4, GROUP_TITLE)
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
