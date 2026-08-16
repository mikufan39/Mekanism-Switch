import json
from pathlib import Path

DUMP = Path(r"D:/Mikufan/Documents/meks/run/config/ProjectE/pregenerated_emc.json")
OUT = Path(r"D:/Mikufan/Documents/meks/src/main/resources/data/meks/sv/emc_preset.json")

data = json.loads(DUMP.read_text(encoding="utf-8"))
preset = []
seen = set()
for entry in data:
    if "data" in entry:
        continue
    item = entry.get("item", "")
    if not (item.startswith("minecraft:") or item.startswith("mekanism:")):
        continue
    if item in seen:
        continue
    seen.add(item)
    preset.append({"item": item, "emc": entry["emc"]})

preset.sort(key=lambda e: (e["item"].split(":")[0], e["item"]))
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(preset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("wrote", len(preset), "entries ->", OUT)
