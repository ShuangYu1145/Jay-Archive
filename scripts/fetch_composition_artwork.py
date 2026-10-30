"""Refresh artwork for the reviewed composition catalogue (never run at build time).

Only exact song and artist matches are accepted; unavailable artwork remains empty.
Dates and composition credits are never taken from search results or overwritten.
"""
import concurrent.futures
import ctypes
import json
from pathlib import Path
import re
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
CATALOG = ASSETS / "compositions.json"


def normalize(value):
    # The maintenance environment is Windows; normalize traditional search metadata.
    buffer = ctypes.create_unicode_buffer(len(value) * 3 + 8)
    ctypes.windll.kernel32.LCMapStringEx("zh-CN", 0x02000000, value, len(value), buffer, len(buffer), None, None, 0)
    return re.sub(r"[\W_]", "", buffer.value).casefold()


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": "JayArchive-CatalogReview/0.5"})
    with urllib.request.urlopen(request, timeout=15) as response:
        return response.read()


def fetch(song):
    if song.get("coverUrl"):
        return song
    try:
        query = urllib.parse.urlencode({"term": song["traditional"]["artist"] + " " + song["traditional"]["title"],
                                       "country": "tw", "entity": "song", "limit": 25, "lang": "zh_tw"})
        results = json.loads(get("https://itunes.apple.com/search?" + query))["results"]
        matches = [item for item in results
                   if normalize(item.get("trackName", "")) == normalize(song["title"])
                   and normalize(item.get("artistName", "")) == normalize(song["artist"])]
        matches.sort(key=lambda item: normalize(item.get("collectionName", "")) != normalize(song["album"]))
        if matches:
            item = matches[0]
            url = re.sub(r"\d+x\d+bb", "600x600bb", item["artworkUrl100"])
            song.update(coverUrl=url, artworkSourceUrl=item["trackViewUrl"])
    except Exception as error:
        print(song["id"], type(error).__name__, flush=True)
    return song


if __name__ == "__main__":
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        catalog["songs"] = list(pool.map(fetch, catalog["songs"]))
    CATALOG.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Artwork: {sum(bool(song.get('coverUrl')) for song in catalog['songs'])}/{len(catalog['songs'])}")
