"""Review Apple Music TW release metadata; never fetch lyrics or run during a build.

Studio editions remain the separately reviewed JVR catalogue. Dates here identify
the TW digital edition, which can differ from the first physical CD release.
"""
import concurrent.futures
import datetime
import json
from pathlib import Path
import re
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
RESEARCH = ROOT / "research"
BASE = "https://itunes.apple.com/lookup?country=tw&"

def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": "JayArchive-CatalogReview/0.2"})
    with urllib.request.urlopen(request, timeout=40) as response:
        return response.read()

def fetch(item):
    ident = str(item["collectionId"])
    raw = get(BASE + f"id={ident}&entity=song&limit=200")
    (RESEARCH / f"apple-{ident}.json").write_bytes(raw)
    data = json.loads(raw)["results"]
    songs = sorted([x for x in data if x.get("wrapperType") == "track"],
                   key=lambda x: (x.get("discNumber", 1), x["trackNumber"]))
    assert len(songs) == item["trackCount"], (ident, len(songs), item["trackCount"])
    name = item["collectionName"]
    if ident in {"536743616", "1485210180", "535913600", "536637791", "1108410359", "536686684"}:
        category = "LIVE"
    elif ident in {"536110584", "536108118", "535756564", "536129008"}:
        category = "EP"
    elif "Single" in name:
        category = "SINGLE"
    else:
        category = "OTHER"
    image = re.sub(r"\d+x\d+bb", "600x600bb", item["artworkUrl100"])
    asset = f"covers/apple-{ident}.jpg"
    (ASSETS / asset).write_bytes(get(image))
    return {"id": "apple-" + ident, "title": re.sub(r" - (Single|EP)$", "", name),
            "englishTitle": "", "category": category,
            "releaseDate": item["releaseDate"][:10], "sourceDateRaw": item["releaseDate"],
            "sourceUrl": item["collectionViewUrl"], "sourceLabel": "Apple Music 台湾目录",
            "coverUrl": image, "coverAsset": asset,
            "dateNote": "日期与曲序采用 Apple Music 台湾数字版目录；不代表首版实体 CD 日期。不同地区、CD/DVD 与数字版收录可能不同。",
            "summary": f"Apple Music 台湾目录收录的{name}，共 {len(songs)} 首曲目。下方依该数字发行版本的碟片号、曲目号排序；现场录音与录音室版本分开归类。",
            "tracks": [{"title": x["trackName"], "disc": x.get("discNumber", 1),
                        "sourceTrackNumber": x["trackNumber"]} for x in songs]}

if __name__ == "__main__":
    data = json.loads(get(BASE + "id=300117743&entity=album&limit=200"))
    (RESEARCH / "apple-albums.json").write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    # Explicit exclusion avoids duplicating or silently replacing reviewed studio editions.
    studio = {944321428,587743633,1118757859,535739206,535790918,1632190508,536009641,
              536285027,535824731,536161722,536030690,536114662,1887246588,535754551,536247746,1624000713}
    selected = [x for x in data["results"] if x.get("wrapperType") == "collection"
                and x["collectionId"] not in studio]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        albums = list(pool.map(fetch, selected))
    albums.sort(key=lambda x: x["releaseDate"], reverse=True)
    (ASSETS / "extended-albums.json").write_text(json.dumps({"verifiedAt": "2026-10-17", "albums": albums},
                                                            ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print({kind: sum(x["category"] == kind for x in albums) for kind in ["LIVE", "EP", "SINGLE", "OTHER"]})
