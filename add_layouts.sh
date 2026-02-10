#!/bin/bash

DOC_FILE="PROJECT_DOCUMENTATION.md"
RES_DIR="app/src/main/res/layout"

# Add main activity layouts
for file in "$RES_DIR"/activity_*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "#### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```xml' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

# Add item layouts
echo "" >> "$DOC_FILE"
echo "### Item Layouts" >> "$DOC_FILE"
echo "" >> "$DOC_FILE"

for file in "$RES_DIR"/item_*.xml; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "#### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```xml' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

echo "Layouts added successfully!"
