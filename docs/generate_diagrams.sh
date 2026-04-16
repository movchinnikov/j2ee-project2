#!/bin/bash
# Generate PNG from all PlantUML files in docs/diagrams/
# Usage: bash docs/generate_diagrams.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIAGRAMS_DIR="$SCRIPT_DIR/diagrams"

echo "=== Cleaning Platform — PlantUML PNG Export ==="
echo "Diagrams directory: $DIAGRAMS_DIR"

# Check for plantuml
if ! command -v plantuml &>/dev/null; then
    echo "❌ plantuml not found in PATH"
    echo "Install with: brew install plantuml"
    echo "Or: sudo apt-get install plantuml"
    exit 1
fi

echo "✅ Found plantuml: $(which plantuml)"
echo ""

# Export all puml files to PNG
for f in "$DIAGRAMS_DIR"/*.puml; do
    echo "Exporting: $(basename "$f")"
    plantuml -tpng "$f"
done

echo ""
echo "✅ Done. Generated PNGs:"
ls -lh "$DIAGRAMS_DIR"/*.png
