"""DaftPorn scraper — extracts direct .mp4 URLs from extreme-videos detail pages via homepage."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from core import create_stream, fetch_with_timeout  # noqa: E402

GROUP_TITLE = "DaftPorn"
MAX_VIDEOS = 30
UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"


def scrape_daftporn():
    results = []
    all_videos = {}
    html = fetch_with_timeout("https://www.daftporn.com/", 15000, {"User-Agent": UA})
    if not html:
        return results

    for m in re.finditer(r'href="(https?://www\.daftporn\.com/extreme-videos/[^"]*\.php)"[^>]*title="([^"]+)"', html):
        video_url = m.group(1)
        title = m.group(2)
        if video_url in all_videos:
            continue

        video_html = fetch_with_timeout(video_url, 15000, {"User-Agent": UA})
        if not video_html:
            continue

        sm = re.search(r'<source[^>]+src="(https?://[^"]+\.mp4[^"]*)"', video_html)
        if sm:
            mp4_url = sm.group(1).split("?")[0]
            all_videos[video_url] = {"title": title, "mp4": mp4_url}
            if len(all_videos) >= MAX_VIDEOS:
                break

    streams = []
    for v in list(all_videos.values())[:MAX_VIDEOS]:
        s = create_stream(v["title"], v["mp4"], GROUP_TITLE)
        s.referrer = "https://www.daftporn.com/"
        s.user_agent = UA
        streams.append(s)
    if streams:
        results.append({"group_title": GROUP_TITLE, "streams": streams})
    return results


if __name__ == "__main__":
    for r in scrape_daftporn():
        for s in r["streams"]:
            print(f'#EXTINF:-1 group-title="{s.group_title}",{s.title}')
            print(s.url)
