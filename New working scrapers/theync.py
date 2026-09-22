"""TheYNC scraper — ported from NettSmuttProvider.js (scrapeTheYNC).

Resolves get_file URLs from TheYNC latest-updates + individual video pages.
TheYNC is behind Cloudflare fingerprinting that blocks plain requests, so when
the direct fetch fails it retries through the CF proxy worker (CF_PROXY_URL env,
same trick as the JS port). Clips play back via the worker proxy.
"""
import os
import re
import sys
from urllib.parse import quote

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "TheYNC"
MAX_VIDEOS = 100
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
CF_PROXY_URL = os.environ.get("CF_PROXY_URL", "")


def _worker_proxy_url(url):
    if not CF_PROXY_URL:
        return url
    sep = "&" if "?" in CF_PROXY_URL else "?"
    return f"{CF_PROXY_URL}{sep}url={quote(url, safe='')}"


def _fetch(url, headers=None, timeout_ms=20000, attempts=2, via_worker=False):
    target = _worker_proxy_url(url) if via_worker else url
    hdrs = {"User-Agent": UA, "Referer": "https://theync.com/"}
    if headers:
        hdrs.update(headers)
    for _ in range(attempts):
        html = fetch_with_timeout(target, timeout_ms, hdrs)
        if html:
            return html
    return None


def scrape_theync():
    results = []
    html = _fetch("https://theync.com/latest-updates/", attempts=2)
    if not html and CF_PROXY_URL:
        html = _fetch("https://theync.com/latest-updates/", attempts=2, via_worker=True)

    if not html:
        return results

    resolved = {}
    mp4_re = re.compile(r"https://theync\.com/get_file/\d+/[^\"'\s]+", re.I)
    for m in mp4_re.finditer(html):
        url = m.group(0)
        if url in resolved:
            continue
        pos = html.find(url)
        nearby = html[max(0, pos - 500):pos]
        t = re.search(r'alt="([^"]+)"', nearby, re.I) or re.search(r"<title>([^<]*)</title>", nearby, re.I)
        resolved[url] = t.group(1).strip() if t else "TheYNC Video"

    video_urls = list({
        m.group(1).split("#")[0]
        for m in re.finditer(r'href="(https://theync\.com/video/\d+[^"]*)"', html, re.I)
    })

    for video_url in video_urls:
        if len(resolved) >= MAX_VIDEOS:
            break
        vhtml = _fetch(video_url, attempts=2)
        if not vhtml and CF_PROXY_URL:
            vhtml = _fetch(video_url, attempts=2, via_worker=True)
        if not vhtml:
            continue
        gf = re.search(r"https://theync\.com/get_file/\d+/[^\"'\s]+", vhtml, re.I)
        if gf and gf.group(0) not in resolved:
            t = re.search(r"<title>([^<]*)</title>", vhtml, re.I)
            resolved[gf.group(0)] = t.group(1).replace(" — TheYNC", "").strip() if t else "TheYNC Video"

    streams = []
    for url, title in list(resolved.items())[:MAX_VIDEOS]:
        s = create_stream(title, url, GROUP_TITLE)
        s.referrer = "https://theync.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_theync():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)