"""JeffsModels scraper — extracts 10-second preview clips from listing page.

Site: https://jeffsmodels.com/
Previews: https://fast-media.roguebucks.com/jeffsmodels.com/tour03/updates/{id}/10_sec.mp4
"""
import html as html_mod
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "JeffsModels"
MAX_VIDEOS = 50
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_jeffsmodels():
    results = []
    html = fetch_with_timeout("https://jeffsmodels.com/", timeout_ms=15000, headers={"User-Agent": UA})
    if not html:
        return results

    mp4s = re.findall(
        r"(https?://fast-media\.roguebucks\.com/jeffsmodels\.com/tour03/updates/\d+/10_sec\.mp4)",
        html,
        re.I,
    )
    mp4s = sorted(set(mp4s))[:MAX_VIDEOS]

    # Extract titles from update links
    title_map = {}
    for m in re.finditer(
        r'href="(/update/\d+/[^"]*)"[^>]*>(.*?)</a>',
        html,
        re.S,
    ):
        path = m.group(1).split("?")[0]
        raw_title = m.group(2).strip()
        raw_title = re.sub(r"<[^>]+>", "", raw_title)
        raw_title = html_mod.unescape(raw_title).strip()
        if raw_title:
            title_map[path] = raw_title

    streams = []
    for url in mp4s:
        vid_id = url.split("/")[-2]
        title = title_map.get(f"/update/{vid_id}/", f"JeffsModels Update {vid_id}")
        s = create_stream(title, url, GROUP_TITLE)
        s.referrer = "https://jeffsmodels.com/"
        s.user_agent = UA
        streams.append(s)

    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_jeffsmodels():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
