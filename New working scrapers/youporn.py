"""YouPorn scraper — extracts token-protected MP4 URLs from video watch pages."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "YouPorn"
MAX_VIDEOS = 20
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
BASE = "https://www.you-porn.com"


def scrape_youporn():
    results = []
    all_videos = {}

    html = fetch_with_timeout(BASE + "/", 15000, {"User-Agent": UA})
    if not html:
        return results

    for m in re.finditer(r'href="(/watch/\d+[^"]*)"', html):
        video_path = m.group(1).rstrip("/")
        video_url = BASE + video_path
        if video_url in all_videos:
            continue

        video_html = fetch_with_timeout(video_url, 15000, {"User-Agent": UA})
        if not video_html:
            continue

        title_m = re.search(r'video_title["\x27:\s]+["\x27]([^"\x27]+)["\x27]', video_html)
        title = title_m.group(1) if title_m else ""
        if not title:
            title_m2 = re.search(r'<title>([^<]+)</title>', video_html)
            if title_m2:
                title = title_m2.group(1).replace(" - Free Porn Videos - YouPorn", "").strip()

        mp4_m = re.search(r'(ev-ph\.ypncdn\.com/videos/[^"\s]+_\d+P_\d+K_\d+_fb\.mp4\?[^\s"<>]+)', video_html)
        if mp4_m:
            mp4_url = "https://" + mp4_m.group(1)
            if not title:
                title = f"YouPorn {video_url}"
            all_videos[video_url] = {"title": title, "mp4": mp4_url}
            if len(all_videos) >= MAX_VIDEOS:
                break

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = BASE
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_youporn():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
