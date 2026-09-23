"""eFukt scraper — extracts latest direct .mp4 clips from eFukt.com."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "eFukt"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_efukt():
    results = []
    all_videos = {}
    for page in range(1, 6):
        if len(all_videos) >= MAX_VIDEOS:
            break
        url = "https://efukt.com/" if page == 1 else f"https://efukt.com/videos/page/{page}/"
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue

        for m in re.finditer(
            r'<a[^>]*href="(https?://efukt\.com/\d+_[^"]+)"[^>]*title="([^"]*)"',
            html,
        ):
            video_url = m.group(1)
            title = m.group(2)
            if video_url in all_videos:
                continue

            video_html = fetch_with_timeout(video_url, 15000, {"User-Agent": UA})
            if not video_html:
                continue

            sm = re.search(
                r'<source[^>]+src="(https?://[^"]+\.mp4[^"]*)"',
                video_html,
            )
            if sm:
                # Preserve the auth query string — eFukt tokens expire quickly
                mp4_url = sm.group(1).replace("&amp;", "&")
                all_videos[video_url] = {"title": title, "mp4": mp4_url}

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = "https://efukt.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_efukt():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
