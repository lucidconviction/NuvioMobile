"""Worldstar scraper — ported from NettSmuttProvider.js (scrapeWorldstar).

Extracts latest direct .mp4 clips from Worldstar's Next.js JSON payloads
(__NEXT_DATA__ / __next_f flight chunks + utLocation), mirroring the JS port.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "Worldstar Hip Hop"
MAX_VIDEOS = 100
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def _extract_videos(text):
    results = []
    seen = set()
    pos = 0
    while pos < len(text):
        t_idx = text.find('"title"', pos)
        if t_idx == -1:
            break
        v_start = text.find('"', t_idx + 7)
        if v_start == -1:
            break
        v_end = v_start + 1
        while v_end < len(text):
            if text[v_end] == '"' and text[v_end - 1] != "\\":
                break
            v_end += 1
        title = text[v_start + 1:v_end]

        u_idx = text.find('"utLocation"', v_end)
        url = None
        if u_idx == -1 or u_idx - v_end > 2000:
            mp4_idx = text.find("mp4", v_end)
            if mp4_idx != -1 and mp4_idx - v_end < 3000:
                u_end = mp4_idx + 4
                while u_end < len(text):
                    if text[u_end] == '"' and text[u_end - 1] != "\\":
                        break
                    u_end += 1
                url = text[mp4_idx:u_end].split("?")[0].split("#")[0]
        else:
            u_start = text.find('"', u_idx + 13)
            if u_start != -1:
                u_end = u_start + 1
                while u_end < len(text):
                    if text[u_end] == '"' and text[u_end - 1] != "\\":
                        break
                    u_end += 1
                url = text[u_start + 1:u_end]

        if url and url.endswith(".mp4") and url not in seen:
            seen.add(url)
            results.append({"title": title.replace('\\"', '"'), "mp4": url})
        pos = v_end + 1
    return results


def scrape_worldstar():
    results = []
    all_videos = {}
    for page in range(1, 6):
        if len(all_videos) >= MAX_VIDEOS:
            break
        url = "https://worldstarhiphop.com/videos/" if page == 1 else f"https://worldstarhiphop.com/videos/page/{page}/"
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue

        json_data = ""
        nd = re.search(r'<script id="__NEXT_DATA__"[^>]*type="application/json"[^>]*>({[\s\S]*?})</script>', html)
        if nd:
            json_data = nd.group(1)
        else:
            for fm in re.finditer(r'<script[^>]*>self\.__next_f\.push\(\[1,"([\s\S]*?)"\]\)</script>', html):
                json_data += fm.group(1).replace('\\"', '"').replace("\\n", "\n").replace("\\\\", "\\")

        if json_data:
            for v in _extract_videos(json_data):
                all_videos.setdefault(v["mp4"], v)
        else:
            stripped = re.sub(r"<[^>]+>", " ", html)
            for m in re.finditer(r"(https?://[^\s\"']+\.mp4)", stripped, re.I):
                all_videos.setdefault(m.group(1), {"title": "Worldstar Video", "mp4": m.group(1)})

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = "https://worldstarhiphop.com/videos/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_worldstar():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)