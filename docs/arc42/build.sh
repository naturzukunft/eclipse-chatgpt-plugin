#!/bin/bash
# Build arc42 documentation to HTML
# Requires: asciidoctor (sudo apt install asciidoctor)
# start it with: ./build.sh 


SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INPUT="$SCRIPT_DIR/arc42.adoc"
OUTPUT="$SCRIPT_DIR/arc42.html"

# Check if asciidoctor is installed
if ! command -v asciidoctor &> /dev/null; then
    echo "Error: asciidoctor not found!"
    echo ""
    echo "Install with:"
    echo "  sudo apt install asciidoctor"
    exit 1
fi

echo "Building arc42 documentation..."

asciidoctor \
    -a toc=left \
    -a toclevels=3 \
    -a sectnums \
    -o "$OUTPUT" \
    "$INPUT"

if [ $? -eq 0 ]; then
    echo "Done: $OUTPUT"
else
    echo "Error: Build failed!"
    exit 1
fi
