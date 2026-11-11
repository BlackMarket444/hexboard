"""Copy the Hexboard layouts into FUTO's layouts folder and offer them for English."""
import pathlib, shutil, sys
src = pathlib.Path(__file__).parent
dst = pathlib.Path(sys.argv[1])
(dst / "Hexboard").mkdir(parents=True, exist_ok=True)
names = []
for f in sorted(src.glob("*.yaml")):
    shutil.copy(f, dst / "Hexboard" / f.name)
    names.append(f.stem)
order = ["hexboard", "hex_qwerty", "hex_qwerty6", "hex_big"]
names = [n for n in order if n in names]
mapping = dst / "mapping.yaml"
out = []
in_en = False
for line in mapping.read_text(encoding="utf-8").splitlines():
    out.append(line)
    if line.startswith("  ") and not line.startswith("   "):
        in_en = line.startswith("  en") and line.strip() != "en_Shaw:"
    elif in_en and line.strip() == "- qwerty":
        out.extend("    - " + n for n in names)   # after qwerty, so the default layout stays qwerty
mapping.write_text("\n".join(out) + "\n", encoding="utf-8")
print("installed", names)
