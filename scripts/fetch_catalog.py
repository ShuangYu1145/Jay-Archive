"""Reproduce the bundled factual catalogue from JVR's official public pages.

Run manually when reviewing a catalogue update; never runs during Gradle builds.
"""
import concurrent.futures
import datetime as dt
import html
import json
from pathlib import Path
import re
import urllib.request
import urllib.parse

ROOT = Path(__file__).resolve().parents[1]
BASE = "https://www.jvrmusic.com"
RESEARCH = ROOT / "research"
ASSETS = ROOT / "app/src/main/assets"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=40) as response:
        return response.read()

def clean(value):
    return html.unescape(re.sub(r"<[^>]+>", "", value)).strip()

def fetch(match):
    album_id, raw_url = match
    url = BASE + html.unescape(raw_url)
    page = get(url).decode("utf-8-sig")
    (RESEARCH / f"album-{album_id}.html").write_text(page, encoding="utf-8")
    title = clean(re.search(r"<h3>(.*?)</h3>", page, re.S)[1])
    english = clean(re.search(r'<h3 class="en">(.*?)</h3>', page, re.S)[1])
    date_raw = re.search(r'class="pub-date" data-value="([^"]+)"', page)[1]
    date = dt.datetime.fromisoformat(date_raw).astimezone(dt.timezone(dt.timedelta(hours=8))).date().isoformat()
    section = page.split('<div class="work-list">')[1].split('</section>')[0]
    tracks = [re.sub(r"^\d+[.．、\s]+", "", clean(x)) for x in re.findall(r"<li>(.*?)</li>", section, re.S)]
    tracks = [x for x in tracks if x]
    assert len(tracks) >= 10, (title, tracks)
    image = html.unescape(re.search(r'<img src="([^"]+)"', page.split('class="work-detail"')[-1])[1]) if 'class="work-detail"' in page else None
    # The album carousel precedes its title. Social icons occur earlier in the page.
    images = re.findall(r'<img src="(/upload/[^\"]+)"', page)
    image = BASE + html.unescape(images[0])
    encoded_image = urllib.parse.quote(image, safe=":/()")
    filename = f"covers/{album_id}.jpg"
    (ASSETS / filename).parent.mkdir(parents=True, exist_ok=True)
    (ASSETS / filename).write_bytes(get(encoded_image))
    return {"id": album_id, "title": title, "englishTitle": english, "releaseDate": date,
            "sourceDateRaw": date_raw, "sourceUrl": url, "coverUrl": encoded_image,
            "coverAsset": filename, "tracks": tracks}

if __name__ == "__main__":
    RESEARCH.mkdir(parents=True, exist_ok=True)
    ASSETS.mkdir(parents=True, exist_ok=True)
    if not (RESEARCH / "albums.html").exists():
        (RESEARCH / "albums.html").write_bytes(get(BASE + "/artist/gallery/list/1150822038412333056?type=album"))
    if not (RESEARCH / "albums-next.html").exists():
        req = urllib.request.Request(BASE + "/artist/gallery/next/1150822038412333056/2?type=album", data=b"", method="POST")
        with urllib.request.urlopen(req, timeout=40) as response:
            (RESEARCH / "albums-next.html").write_bytes(response.read())
    pages = (RESEARCH / "albums.html").read_text(encoding="utf-8-sig") + (RESEARCH / "albums-next.html").read_text(encoding="utf-8-sig")
    urls = re.findall(r'href="(/artist/gallery/detail/(\d+)\?type=album)"', pages)
    ordered = list(dict.fromkeys((album_id, url) for url, album_id in urls))
    assert len(ordered) == 16, len(ordered)
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        albums = list(pool.map(fetch, ordered))
    albums.sort(key=lambda x: x["releaseDate"], reverse=True)
    (ASSETS / "albums.json").write_text(json.dumps({"verifiedAt": "2026-10-17", "albums": albums}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    portrait = BASE + "/upload/1153987416726573056/album_carousel_14.jpg"
    (ASSETS / "jay-portrait.jpg").write_bytes(get(portrait))
    for album in albums:
        print(album["title"], album["releaseDate"], len(album["tracks"]))
