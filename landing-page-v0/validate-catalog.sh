#!/bin/bash

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "        HDIM LANDING PAGE - CONTENT CATALOG & VALIDATION"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 1. IMAGE REFERENCES VALIDATION
echo "🖼️  IMAGE REFERENCES"
echo "──────────────────────────────────────────────────────────────────────"

echo "Checking all Image components in app/page.tsx..."
echo ""

# Extract all Image src paths
IMAGE_REFS=$(grep -oP 'src="/images/[^"]+' app/page.tsx 2>/dev/null | sed 's/src="//' | sort | uniq)

total_images=0
existing_images=0
missing_images=0

for img_path in $IMAGE_REFS; do
    total_images=$((total_images + 1))
    if [ -f "public$img_path" ]; then
        echo "  ✅ $img_path"
        existing_images=$((existing_images + 1))
    else
        echo "  ❌ $img_path (MISSING)"
        missing_images=$((missing_images + 1))
    fi
done

echo ""
echo "Total Image References: $total_images"
echo "Existing Images: $existing_images"
echo "Missing Images: $missing_images"
echo ""

# 2. NAVIGATION LINKS VALIDATION
echo "📍 NAVIGATION LINKS"
echo "──────────────────────────────────────────────────────────────────────"

echo "Internal Pages:"
for page in /demo /explorer /research /downloads; do
    if [ -f "app${page}/page.tsx" ]; then
        echo "  ✅ $page"
    else
        echo "  ❌ $page (MISSING)"
    fi
done

echo ""
echo "Anchor Links (section IDs):"
ANCHOR_LINKS=$(grep -oP 'href="#[^"]+' app/page.tsx 2>/dev/null | sed 's/href="#//' | sort | uniq)
for anchor in $ANCHOR_LINKS; do
    if grep -q "id=\"$anchor\"" app/page.tsx 2>/dev/null; then
        echo "  ✅ #$anchor"
    else
        echo "  ❌ #$anchor (SECTION NOT FOUND)"
    fi
done

echo ""

# 3. SECTION CONTENT AUDIT
echo "📄 CONTENT SECTIONS"
echo "──────────────────────────────────────────────────────────────────────"

echo "Sections with IDs:"
SECTIONS=$(grep -oP 'id="[^"]+' app/page.tsx 2>/dev/null | sed 's/id="//' | grep -v "^$")
for section in $sections; do
    echo "  ✅ #$section"
done
echo ""

# 4. ASSET DIRECTORY INVENTORY
echo "📦 ASSET DIRECTORY INVENTORY"
echo "──────────────────────────────────────────────────────────────────────"

for dir in public/images/*/; do
    if [ -d "$dir" ]; then
        dir_name=$(basename "$dir")
        count=$(find "$dir" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.svg" \) | wc -l)
        echo "  $dir_name/ ($count files)"
        find "$dir" -type f \( -name "*.png" -o -name "*.jpg" -o -name "*.svg" \) -exec basename {} \; | sed 's/^/    • /'
    fi
done

echo ""

# 5. CONTENT CONSISTENCY
echo "🔍 CONTENT CONSISTENCY"
echo "──────────────────────────────────────────────────────────────────────"

echo "Keyword Frequency:"
echo "  FHIR: $(grep -o 'FHIR' app/page.tsx 2>/dev/null | wc -l) mentions"
echo "  CQL: $(grep -o 'CQL' app/page.tsx 2>/dev/null | wc -l) mentions"
echo "  HEDIS: $(grep -o 'HEDIS' app/page.tsx 2>/dev/null | wc -l) mentions"
echo "  care gap: $(grep -oi 'care gap' app/page.tsx 2>/dev/null | wc -l) mentions"
echo "  real-time: $(grep -oi 'real-time' app/page.tsx 2>/dev/null | wc -l) mentions"

echo ""

# 6. CALL-TO-ACTION INVENTORY
echo "💬 CALL-TO-ACTION BUTTONS"
echo "──────────────────────────────────────────────────────────────────────"

echo "Primary CTAs found:"
grep -oP '(Try|Request|Watch|Calculate|Schedule|Start|See|Contact)[^<]+' app/page.tsx 2>/dev/null | grep -v 'className' | sort | uniq | sed 's/^/  • /'

echo ""

# 7. FINAL SUMMARY
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "        VALIDATION SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ $missing_images -eq 0 ]; then
    echo "✅ All image references validated"
else
    echo "❌ $missing_images missing images found"
fi

echo "📊 Total assets deployed: $(find public/images -type f | wc -l) files"
echo "🚀 Live URL: https://www.healthdatainmotion.com"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

