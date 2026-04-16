#!/usr/bin/env python3
"""
All-in-one generator:
  1. Generates PNG from PUML files via PlantUML online API
  2. Regenerates the PDF report

Usage:
  pip3 install reportlab requests
  python3 docs/generate_all.py
"""
import os, sys, zlib, base64, struct, subprocess

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIAGRAMS_DIR = os.path.join(BASE_DIR, "docs", "diagrams")

# ─────────────────────────────────────────────────────────
# STEP 1 — Export PUML → PNG
# ─────────────────────────────────────────────────────────

def encode_plantuml(puml_text: str) -> str:
    """Encode PlantUML text for the online API using PlantUML's modified Base64."""
    compressed = zlib.compress(puml_text.encode("utf-8"), 9)[2:-4]
    chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"
    res = []
    for i in range(0, len(compressed), 3):
        chunk = compressed[i:i+3]
        b0 = chunk[0] if len(chunk) > 0 else 0
        b1 = chunk[1] if len(chunk) > 1 else 0
        b2 = chunk[2] if len(chunk) > 2 else 0
        res.append(chars[(b0 >> 2) & 0x3F])
        res.append(chars[((b0 & 0x3) << 4) | ((b1 >> 4) & 0xF)])
        res.append(chars[((b1 & 0xF) << 2) | ((b2 >> 6) & 0x3)])
        res.append(chars[b2 & 0x3F])
    return "".join(res)


def generate_png_via_api(puml_path: str, out_path: str):
    """Fetch PNG from PlantUML online server."""
    try:
        import requests
    except ImportError:
        print("  requests not installed. Run: pip3 install requests")
        return False

    with open(puml_path, "r", encoding="utf-8") as f:
        text = f.read()

    encoded = encode_plantuml(text)
    url = f"https://www.plantuml.com/plantuml/png/{encoded}"
    print(f"  Fetching: {url[:80]}...")

    try:
        r = requests.get(url, timeout=60)
        if r.status_code == 200 and r.headers.get("content-type", "").startswith("image/"):
            with open(out_path, "wb") as f:
                f.write(r.content)
            size = os.path.getsize(out_path)
            print(f"  ✅ Saved: {os.path.basename(out_path)} ({size:,} bytes)")
            return True
        else:
            print(f"  ❌ HTTP {r.status_code} for {os.path.basename(puml_path)}")
            return False
    except Exception as e:
        print(f"  ❌ Error: {e}")
        return False


def generate_png_via_local(puml_path: str, out_path: str) -> bool:
    """Try local plantuml command."""
    import shutil
    if not shutil.which("plantuml"):
        return False
    result = subprocess.run(
        ["plantuml", "-tpng", "-o", os.path.dirname(out_path), puml_path],
        capture_output=True, text=True
    )
    if result.returncode == 0 and os.path.exists(out_path):
        print(f"  ✅ Local plantuml: {os.path.basename(out_path)}")
        return True
    return False


NEW_DIAGRAMS = [
    ("06_sequence_assign_role.puml",  "06_sequence_assign_role.png"),
    ("07_sequence_authorization.puml", "07_sequence_authorization.png"),
]

print("=" * 60)
print("  STEP 1 — Generating PNG from new PlantUML diagrams")
print("=" * 60)

for puml_name, png_name in NEW_DIAGRAMS:
    puml_path = os.path.join(DIAGRAMS_DIR, puml_name)
    out_path  = os.path.join(DIAGRAMS_DIR, png_name)

    if not os.path.exists(puml_path):
        print(f"  SKIP: {puml_name} not found")
        continue

    if os.path.exists(out_path):
        print(f"  EXISTS: {png_name} — skipping (delete to regenerate)")
        continue

    print(f"\nProcessing: {puml_name}")

    # Try local plantuml first, then API
    if not generate_png_via_local(puml_path, out_path):
        generate_png_via_api(puml_path, out_path)

print()

# ─────────────────────────────────────────────────────────
# STEP 2 — Regenerate PDF report
# ─────────────────────────────────────────────────────────
print("=" * 60)
print("  STEP 2 — Regenerating PDF report")
print("=" * 60)

report_script = os.path.join(BASE_DIR, "docs", "generate_report.py")
result = subprocess.run([sys.executable, report_script], capture_output=False)
if result.returncode != 0:
    print("  ❌ PDF generation failed")
    sys.exit(1)

print()
print("=" * 60)
print("  ALL DONE")
print("=" * 60)
print(f"  Diagrams : {DIAGRAMS_DIR}/")
pdf_path = os.path.join(BASE_DIR, "docs", "report", "Cleaning_Platform_Report.pdf")
if os.path.exists(pdf_path):
    print(f"  Report   : {pdf_path} ({os.path.getsize(pdf_path):,} bytes)")
print(f"  HTML     : {os.path.join(BASE_DIR, 'docs', 'presentation.html')}")
