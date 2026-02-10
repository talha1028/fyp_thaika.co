#!/bin/bash

DOC_FILE="PROJECT_DOCUMENTATION.md"
SRC_DIR="app/src/main/java/com/example/madproject"
RES_DIR="app/src/main/res"

# Activities
echo "" >> "$DOC_FILE"
echo "## Activities" >> "$DOC_FILE"
echo "" >> "$DOC_FILE"

for file in "$SRC_DIR"/*Activity.java; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```java' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo "---" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

# Firebase Managers
echo "" >> "$DOC_FILE"
echo "## Firebase Managers" >> "$DOC_FILE"
echo "" >> "$DOC_FILE"

for file in "$SRC_DIR"/firebase/*.java; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```java' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo "---" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

# Adapters
echo "" >> "$DOC_FILE"
echo "## Adapters" >> "$DOC_FILE"
echo "" >> "$DOC_FILE"

for file in "$SRC_DIR"/adapters/*.java; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```java' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo "---" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

# Models
echo "" >> "$DOC_FILE"
echo "## Models" >> "$DOC_FILE"
echo "" >> "$DOC_FILE"

for file in "$SRC_DIR"/models/*.java; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```java' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo "---" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

# Helpers
echo "" >> "$DOC_FILE"
echo "## Helpers" >> "$DOC_FILE"
echo "" >> "$DOC_FILE"

for file in "$SRC_DIR"/helpers/*.java; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "### $filename" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo '```java' >> "$DOC_FILE"
        cat "$file" >> "$DOC_FILE"
        echo '```' >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
        echo "---" >> "$DOC_FILE"
        echo "" >> "$DOC_FILE"
    fi
done

echo "Documentation generated successfully!"
