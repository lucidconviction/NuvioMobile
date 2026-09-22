"""HorribleVideos scraper — extracts latest direct .mp4 clips."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "HorribleVideos"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_horriblevideos():
    results = []
    all_videos = {}
    for page in range(1, 10):
        if len(all_videos) >= MAX_VIDEOS:
            break
        url = f"https://horriblevideos.com/page{page}.html" if page > 1 else "https://horriblevideos.com/"
        html = fetch_with_timeout(url, 15000, {"User-Agent": UA})
        if not html:
            continue

        for m in re.finditer(
            r'<a[^>]*href="(https?://horriblevideos\.com/video/[^"]+)"[^>]*title="([^"]*)"',
            html,
        ):
            video_url = m.group(1)
            title = m.group(2)
            if video_url in all_videos:
                continue

            vid_id = re.search(r'-(\d+)\.html', video_url)
            if not vid_id:
                continue
            embed_url = f"https://horriblevideos.com/embed/{vid_id.group(1)}"

            embed_html = fetch_with_timeout(embed_url, 15000, {"User-Agent": UA})
            if not embed_html:
                continue

            sm = re.search(r'<source[^>]+src="(https?://[^"]+\.mp4[^"]*)"', embed_html)
            if sm:
                all_videos[video_url] = {"title": title, "mp4": sm.group(1)}

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = "https://horriblevideos.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_horriblevideos():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
