#!/bin/bash
# =============================================================================
# HDIM Marketing Image Generation Script - Multi-Platform Support
# Supports: Gemini 2.5 Pro, DALL-E 3, Midjourney exports, Stable Diffusion exports
# =============================================================================
#
# Usage:
#   ./generate-marketing-images.sh --platform gemini --all
#   ./generate-marketing-images.sh --platform dalle --category linkedin
#   ./generate-marketing-images.sh --export-midjourney
#   ./generate-marketing-images.sh --export-sd
#   ./generate-marketing-images.sh --list
#

set -e

# ============================================================================
# CONFIGURATION
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/generated-images"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# API Keys (can be overridden by environment variables)
GOOGLE_API_KEY="${GOOGLE_API_KEY:-AIzaSyBJKY_Hml7wvwxdppZQjET_imtwnAELhck}"
export GOOGLE_API_KEY

# Brand Colors
PRIMARY="#1E3A5F"      # Deep Navy Blue
SECONDARY="#00A9A5"    # Teal
SUCCESS="#2E7D32"      # Green
WARNING="#E65100"      # Orange

# ============================================================================
# PROMPTS DATABASE
# ============================================================================

# Associative arrays for prompts
declare -A PROMPTS
declare -A CATEGORIES
declare -A DIMENSIONS
declare -A NAMES

# LinkedIn Ads
NAMES["AD-01"]="Pain Point - Integration Nightmare"
CATEGORIES["AD-01"]="linkedin"
DIMENSIONS["AD-01"]="1200x1200"
PROMPTS["AD-01"]="Create a photorealistic 4K marketing image for LinkedIn advertising targeting hospital CIOs. Scene: A stressed hospital IT professional (male, 40s, professional attire) standing in front of a massive wall of tangled network cables and server racks. Red warning lights blink throughout. The environment is chaotic - papers scattered, multiple monitors showing error messages. The mood is frustration and overwhelm. Style: Corporate photography with dramatic lighting, cinematic quality. Colors: Dark blues, warning reds, harsh fluorescent lighting. Composition: Subject left of center, clear space upper-right for text overlay. Resolution: High detail, 4K quality. This represents the \$500K/year integration nightmare that hospital IT teams face daily."

NAMES["AD-02"]="Solution - Modern Dashboard"
CATEGORIES["AD-02"]="linkedin"
DIMENSIONS["AD-02"]="1200x1200"
PROMPTS["AD-02"]="Create a photorealistic 4K marketing image showing healthcare IT transformation success. Scene: A confident healthcare CIO (female, 50s, executive attire) in a modern, bright hospital command center. She stands before a wall of organized monitors showing green status indicators and smooth data flow visualizations. Natural light, glass walls, modern furniture, calm atmosphere. Mood: Confidence, control, success, relief. Style: Modern corporate photography, Apple-esque clean aesthetic. Colors: Clean whites, deep navy blue (${PRIMARY}), teal accents (${SECONDARY}). Composition: Subject right of center, clean monitors behind, space on left for text. This represents the future after implementing HDIM - calm, controlled, efficient."

NAMES["AD-03"]="Before/After Split Screen"
CATEGORIES["AD-03"]="linkedin"
DIMENSIONS["AD-03"]="1200x628"
PROMPTS["AD-03"]="Create a dramatic 4K split-screen comparison image showing healthcare IT transformation. LEFT SIDE - BEFORE (Legacy): Chaotic server room, tangled cables, old monitors with error messages, stressed IT worker on phone, red warning lights, cluttered space, subtle text: 3.5 DAYS. RIGHT SIDE - AFTER (HDIM): Clean modern IT center, unified dashboard, curved monitor, relaxed professional smiling, green status indicators, subtle text: 6 HOURS. Sharp vertical dividing line with subtle glow effect. Style: High-end commercial photography, magazine quality. Colors: Left side dark/red, right side bright with teal (${SECONDARY}) accents."

NAMES["AD-04"]="CMS 2026 Deadline Urgency"
CATEGORIES["AD-04"]="linkedin"
DIMENSIONS["AD-04"]="1200x1200"
PROMPTS["AD-04"]="Create a 4K marketing image conveying urgency around the CMS 2026 healthcare compliance deadline. Scene: Hospital boardroom with concerned but professional executives. Large screen displays JANUARY 1, 2026 with countdown timer. Documents with CMS-0057-F visible on table. Mood is serious professionalism, not panic. Key Elements: Large countdown visualization showing 2026, concerned executive expressions, regulatory documents visible, modern hospital boardroom. Colors: Deep blues, urgent amber/gold for countdown elements. Style: Corporate boardroom photography. Composition: Wide shot showing room, screen prominent."

NAMES["AD-05"]="ROI Cost Savings"
CATEGORIES["AD-05"]="linkedin"
DIMENSIONS["AD-05"]="1200x1200"
PROMPTS["AD-05"]="Create a 4K marketing image visualizing significant healthcare IT cost savings. Scene: Hospital executive at modern desk, pleased expression, reviewing tablet with positive financial data. Large monitor behind shows upward-trending ROI graph. Modern executive office, hospital campus visible through windows. Subtle \$420K SAVED visible on screen. Mood: Success, satisfaction, financial confidence. Style: Executive portrait photography, warm professional lighting. Colors: Navy blue (${PRIMARY}), teal (${SECONDARY}), gold/green positive indicators. Composition: Executive 2/3 frame, metrics visible behind."

NAMES["AD-06"]="CIO Leadership"
CATEGORIES["AD-06"]="linkedin"
DIMENSIONS["AD-06"]="1200x1200"
PROMPTS["AD-06"]="Create a 4K image targeting hospital CIOs showing strategic leadership. Scene: Confident CIO (male, 50s, navy suit) presenting to hospital board in modern boardroom. Screen shows technology roadmap with FHIR Compliance, AI Integration, Cost Reduction milestones. Board members engaged. Hospital operations visible through glass walls. Mood: Leadership, strategic vision, confidence. Style: Executive boardroom photography. Colors: Navy suits, warm lighting, teal (${SECONDARY}) screen accents. Composition: CIO presenting, board engaged, strategy visible on screen."

NAMES["AD-07"]="CMIO Clinical Tech Bridge"
CATEGORIES["AD-07"]="linkedin"
DIMENSIONS["AD-07"]="1200x1200"
PROMPTS["AD-07"]="Create a 4K image targeting hospital CMIOs - bridging clinical and technology. Scene: CMIO (female physician, white coat, 45-55) at intersection of clinical care and technology. Split composition: one side shows patient care (stethoscope, caring interaction), other side shows clean data dashboards. CMIO bridges both confidently. Mood: Bridge-builder, clinical excellence meets technology. Style: Professional healthcare photography. Colors: White coat prominent, navy (${PRIMARY}) tech elements, warm clinical lighting. Composition: CMIO centered, bridging two worlds."

NAMES["AD-08"]="VP IT Operations"
CATEGORIES["AD-08"]="linkedin"
DIMENSIONS["AD-08"]="1200x1200"
PROMPTS["AD-08"]="Create a 4K image targeting VP of IT showing operational excellence. Scene: VP of IT (40s, business casual) in modern network operations center. All monitors show green healthy status. Calm, organized environment. Team works normally in background, no stress. Mood: Control, efficiency, calm confidence. Style: Corporate IT environment photography. Colors: Modern lighting, green status indicators, teal (${SECONDARY}) accents. Composition: VP in foreground, healthy operations behind."

# Hero Images
NAMES["HERO-01"]="Data Flow Abstract"
CATEGORIES["HERO-01"]="hero"
DIMENSIONS["HERO-01"]="1920x1080"
PROMPTS["HERO-01"]="Create a stunning abstract 4K hero image for a healthcare technology website. Visual: Elegant visualization of healthcare data flowing between connected systems. Network of glowing nodes (hospitals, clinics, payers) connected by smooth flowing data streams in teal (${SECONDARY}). Background transitions from deep navy (${PRIMARY}) to lighter blue. Subtle medical icons integrated. Mood: Innovation, connection, seamless flow. Style: Abstract digital art, premium tech aesthetic like Stripe or Notion. Colors: Navy (${PRIMARY}) gradient, teal (${SECONDARY}) glowing streams. Composition: Data flows left to right, open space left for text overlay."

NAMES["HERO-02"]="Platform Architecture"
CATEGORIES["HERO-02"]="hero"
DIMENSIONS["HERO-02"]="1920x1080"
PROMPTS["HERO-02"]="Create a 4K isometric visualization of a healthcare integration platform. Visual: Central hub platform glowing in teal (${SECONDARY}) with AI symbolism. Connected healthcare systems as clean modern blocks: Epic, Cerner, payers, labs, pharmacies. Data flows as smooth streams through central hub. Platform-centric, not point-to-point. Style: 3D isometric illustration, modern SaaS infographic. Colors: Navy (${PRIMARY}) background, teal (${SECONDARY}) hub, white systems. Composition: Central hub with radial connections, symmetrical, clean."

NAMES["HERO-03"]="AI Healthcare Fusion"
CATEGORIES["HERO-03"]="hero"
DIMENSIONS["HERO-03"]="1920x1080"
PROMPTS["HERO-03"]="Create a 4K abstract image representing AI-powered healthcare technology. Visual: Neural network pattern forming subtle brain/human outline. Medical symbols (caduceus, heart, DNA) integrated into AI visualization. Glowing connections suggesting intelligence. Balance of human care and machine intelligence. Mood: Innovation, intelligence, care. Style: Abstract digital art, premium tech aesthetic. Colors: Navy (${PRIMARY}) background, teal (${SECONDARY}) and white glows. Composition: Centered, room for text on sides."

# Pitch Deck
NAMES["PITCH-01"]="Problem Maze"
CATEGORIES["PITCH-01"]="pitch"
DIMENSIONS["PITCH-01"]="1920x1080"
PROMPTS["PITCH-01"]="Create a 4K conceptual image for investor pitch deck Problem slide. Visual: Complex maze viewed from above. Tiny figures (IT staff) lost and struggling. Dead ends, wrong turns visible. Some paths glow red showing failures. Maze made of server-like structures and cable paths. Mood: Complexity, frustration, scale of problem. Style: Conceptual illustration, investor presentation quality. Colors: Grays, muted blues, red problem areas. Composition: Birds eye view, dramatic lighting."

NAMES["PITCH-02"]="Solution Breakthrough"
CATEGORIES["PITCH-02"]="pitch"
DIMENSIONS["PITCH-02"]="1920x1080"
PROMPTS["PITCH-02"]="Create a 4K conceptual image for investor pitch deck Solution slide. Visual: Same maze concept but now with clear glowing teal (${SECONDARY}) path cutting through. Or maze dissolving into simple elegant bridge. Solution as beacon of light through complexity. Mood: Clarity, breakthrough, elegance. Style: Conceptual illustration matching problem slide. Colors: Solution path teal (${SECONDARY}), complexity fading gray. Composition: Visual transformation story."

NAMES["PITCH-03"]="Market Opportunity"
CATEGORIES["PITCH-03"]="pitch"
DIMENSIONS["PITCH-03"]="1920x1080"
PROMPTS["PITCH-03"]="Create a 4K conceptual image visualizing \$24.8B market opportunity. Visual: Expansive landscape with sunrise suggesting growth, or expanding universe of opportunity. Healthcare symbols throughout. Growth trajectory visible. Enormous scale and potential. Mood: Massive opportunity, growth, optimism. Style: Abstract conceptual art, premium investor presentation. Colors: Navy (${PRIMARY}) space, teal (${SECONDARY}) and gold for opportunity. Composition: Expansive, opening toward viewer."

NAMES["PITCH-04"]="Traction Momentum"
CATEGORIES["PITCH-04"]="pitch"
DIMENSIONS["PITCH-04"]="1920x1080"
PROMPTS["PITCH-04"]="Create a 4K conceptual image showing startup momentum and traction. Visual: Stylized rocket/spacecraft in flight - professional not cartoonish. Clear upward trajectory. Trail shows milestones. Stars ahead show goals. Healthcare/tech elements incorporated. Mood: Momentum, achievement, velocity. Style: Stylized conceptual illustration, premium. Colors: Navy (${PRIMARY}) space, teal (${SECONDARY}) trajectory. Composition: Diagonal upward movement."

# Social Proof
NAMES["SOCIAL-01"]="Hospital Success"
CATEGORIES["SOCIAL-01"]="social"
DIMENSIONS["SOCIAL-01"]="1200x1200"
PROMPTS["SOCIAL-01"]="Create a 4K image representing hospital transformation success. Scene: Modern hospital exterior at golden hour. Happy diverse healthcare workers (doctor, nurse, IT professional) walking together confidently. Digital elements suggest tech advancement. Clear sky, bright future feeling. Mood: Success, optimism, teamwork. Style: Commercial healthcare photography. Colors: Warm golden hour, whites, teal (${SECONDARY}) accents. Composition: Hospital backdrop, people foreground."

NAMES["SOCIAL-02"]="IT Team Celebration"
CATEGORIES["SOCIAL-02"]="social"
DIMENSIONS["SOCIAL-02"]="1200x1200"
PROMPTS["SOCIAL-02"]="Create a 4K image of hospital IT team celebrating success. Scene: Modern IT department. Diverse team of 4-5 professionals celebrating - high fives, genuine smiles, looking at positive metrics. Monitors show green status. Clean, organized environment. Mood: Achievement, genuine celebration. Style: Corporate photography, authentic emotion. Colors: Bright modern lighting, teal (${SECONDARY}), green indicators. Composition: Team centered, success metrics visible."

# ============================================================================
# FUNCTIONS
# ============================================================================

setup_dirs() {
    mkdir -p "${OUTPUT_DIR}"/{linkedin,hero,pitch,social,exports}
    echo "Output directory: ${OUTPUT_DIR}"
}

print_usage() {
    cat << 'EOF'
╔══════════════════════════════════════════════════════════════════╗
║    HDIM MARKETING IMAGE GENERATOR - MULTI-PLATFORM SUPPORT       ║
╚══════════════════════════════════════════════════════════════════╝

SUPPORTED PLATFORMS:
  • Gemini 2.5 Pro (Google) - API or CLI generation
  • DALL-E 3 (OpenAI) - High quality photorealistic images
  • Midjourney - Export prompts for Discord
  • Stable Diffusion - Export prompts for local generation

USAGE:
    ./generate-marketing-images.sh [OPTIONS]

GENERATION OPTIONS (require API/CLI):
    --platform <gemini|dalle>    Platform to use (default: gemini)
    --model <model>              Model (e.g., gemini-2.5-pro, imagen-3)
    --all                        Generate all images
    --category <cat>             Generate by category
                                 (linkedin, hero, pitch, social)
    --single <id>                Generate single image (e.g., AD-01)

EXPORT OPTIONS (no API needed):
    --export-midjourney          Export prompts for Midjourney
    --export-sd                  Export prompts for Stable Diffusion
    --export                     Export all prompts in all formats
    --list                       List all available prompts
    --help                       Show this help message

EXAMPLES:
    # Generate all LinkedIn ads with Gemini 2.5 Pro
    ./generate-marketing-images.sh --platform gemini --model gemini-2.5-pro --category linkedin

    # Generate single hero image with DALL-E 3
    ./generate-marketing-images.sh --platform dalle --single HERO-01

    # Export prompts for Midjourney (no API needed)
    ./generate-marketing-images.sh --export-midjourney

    # List all prompts
    ./generate-marketing-images.sh --list

ENVIRONMENT VARIABLES:
    GOOGLE_API_KEY    Google AI API key (for Gemini)
    OPENAI_API_KEY    OpenAI API key (for DALL-E 3)

EOF
}

list_prompts() {
    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "                 HDIM MARKETING IMAGE PROMPTS"
    echo "════════════════════════════════════════════════════════════════"
    echo ""
    echo "Total: ${#PROMPTS[@]} prompts"
    echo ""

    for category in linkedin hero pitch social; do
        count=0
        for id in "${!CATEGORIES[@]}"; do
            if [[ "${CATEGORIES[$id]}" == "$category" ]]; then
                ((count++))
            fi
        done
        echo "▸ ${category^^} (${count} images):"
        for id in "${!CATEGORIES[@]}"; do
            if [[ "${CATEGORIES[$id]}" == "$category" ]]; then
                echo "    ${id}: ${NAMES[$id]} [${DIMENSIONS[$id]}]"
            fi
        done | sort
        echo ""
    done
}

generate_with_gemini() {
    local prompt_id="$1"
    local model="${2:-gemini-2.0-flash-exp-image-generation}"

    if [[ -z "${PROMPTS[$prompt_id]}" ]]; then
        echo "Error: Unknown prompt ID '$prompt_id'"
        return 1
    fi

    local category="${CATEGORIES[$prompt_id]}"
    local name="${NAMES[$prompt_id]}"
    local prompt="${PROMPTS[$prompt_id]}"
    local output_file="${OUTPUT_DIR}/${category}/${prompt_id}_${TIMESTAMP}.png"

    echo ""
    echo ">>> Generating with Gemini: $prompt_id - $name"
    echo "    Model: $model"
    echo "    Output: $output_file"

    if [[ -z "$GOOGLE_API_KEY" ]]; then
        echo "    Error: GOOGLE_API_KEY not set"
        return 1
    fi

    # Escape prompt for JSON
    local escaped_prompt="${prompt//\\/\\\\}"
    escaped_prompt="${escaped_prompt//\"/\\\"}"
    escaped_prompt="${escaped_prompt//$'\n'/\\n}"

    # Use Gemini generateContent API for image generation
    local response
    response=$(curl -s "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GOOGLE_API_KEY}" \
        -H "Content-Type: application/json" \
        -d "{
            \"contents\": [{
                \"parts\": [{
                    \"text\": \"Generate this image: ${escaped_prompt}\"
                }]
            }],
            \"generationConfig\": {
                \"responseModalities\": [\"image\", \"text\"],
                \"responseMimeType\": \"text/plain\"
            }
        }" 2>&1)

    # Save response for debugging
    echo "$response" > "${output_file%.png}_response.json"

    # Check for error
    if echo "$response" | grep -q '"error"'; then
        echo "    API Error: $(echo "$response" | grep -o '"message": "[^"]*"' | head -1)"
        echo "    Response saved to: ${output_file%.png}_response.json"
        return 1
    fi

    # Extract base64 image data from generateContent response
    local image_data
    image_data=$(python3 -c "
import sys, json
try:
    data = json.load(open('${output_file%.png}_response.json'))
    candidates = data.get('candidates', [])
    if candidates:
        parts = candidates[0].get('content', {}).get('parts', [])
        for part in parts:
            if 'inlineData' in part:
                print(part['inlineData'].get('data', ''))
                break
except Exception as e:
    pass
" 2>/dev/null)

    if [[ -n "$image_data" && "$image_data" != "" ]]; then
        echo "$image_data" | base64 -d > "$output_file"
        echo "    Success! Saved to: $output_file"
        rm -f "${output_file%.png}_response.json"  # Clean up response file on success
    else
        echo "    Warning: Could not extract image from response"
        echo "    Response saved to: ${output_file%.png}_response.json"
    fi
}

generate_with_dalle() {
    local prompt_id="$1"

    if [[ -z "${PROMPTS[$prompt_id]}" ]]; then
        echo "Error: Unknown prompt ID '$prompt_id'"
        return 1
    fi

    local category="${CATEGORIES[$prompt_id]}"
    local name="${NAMES[$prompt_id]}"
    local prompt="${PROMPTS[$prompt_id]}"
    local dims="${DIMENSIONS[$prompt_id]}"
    local output_file="${OUTPUT_DIR}/${category}/${prompt_id}_dalle_${TIMESTAMP}"

    # Map dimensions to DALL-E sizes
    local size="1024x1024"
    if [[ "$dims" == "1200x628" || "$dims" == "1920x1080" ]]; then
        size="1792x1024"
    fi

    echo ""
    echo ">>> Generating with DALL-E 3: $prompt_id - $name"
    echo "    Size: $size"
    echo "    Output: ${output_file}.png"

    if [[ -z "$OPENAI_API_KEY" ]]; then
        echo "    Error: OPENAI_API_KEY not set"
        echo "    Saving prompt for manual use..."
        cat > "${output_file}.txt" << EOF
Prompt ID: $prompt_id
Name: $name
Size: $size
Timestamp: $TIMESTAMP

========================================
DALL-E 3 PROMPT:
========================================
$prompt

========================================
API PARAMETERS:
========================================
model: dall-e-3
size: $size
quality: hd
style: natural
EOF
        return 1
    fi

    # Call OpenAI API
    local response
    response=$(curl -s https://api.openai.com/v1/images/generations \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $OPENAI_API_KEY" \
        -d "{
            \"model\": \"dall-e-3\",
            \"prompt\": \"${prompt//\"/\\\"}\",
            \"n\": 1,
            \"size\": \"$size\",
            \"quality\": \"hd\",
            \"style\": \"natural\"
        }")

    echo "$response" > "${output_file}.json"

    # Extract image URL
    local image_url
    image_url=$(echo "$response" | grep -o '"url": "[^"]*"' | head -1 | cut -d'"' -f4)

    if [[ -n "$image_url" ]]; then
        echo "    Image URL: $image_url"
        # Download image
        curl -s "$image_url" -o "${output_file}.png"
        echo "    Downloaded to: ${output_file}.png"
    else
        echo "    Warning: Could not extract image URL"
        echo "    Response saved to: ${output_file}.json"
    fi
}

generate_category() {
    local category="$1"
    local platform="$2"
    local model="$3"

    local count=0
    for id in "${!CATEGORIES[@]}"; do
        if [[ "${CATEGORIES[$id]}" == "$category" ]]; then
            ((count++))
        fi
    done

    echo ""
    echo "Generating $count $category images with $platform..."

    for id in "${!CATEGORIES[@]}"; do
        if [[ "${CATEGORIES[$id]}" == "$category" ]]; then
            if [[ "$platform" == "gemini" ]]; then
                generate_with_gemini "$id" "$model"
            elif [[ "$platform" == "dalle" ]]; then
                generate_with_dalle "$id"
            fi
        fi
    done
}

generate_all() {
    local platform="$1"
    local model="$2"

    echo ""
    echo "Generating all ${#PROMPTS[@]} images with $platform..."

    for id in "${!PROMPTS[@]}"; do
        if [[ "$platform" == "gemini" ]]; then
            generate_with_gemini "$id" "$model"
        elif [[ "$platform" == "dalle" ]]; then
            generate_with_dalle "$id"
        fi
    done
}

export_midjourney() {
    local output_file="${OUTPUT_DIR}/exports/midjourney_prompts_${TIMESTAMP}.md"

    cat > "$output_file" << 'HEADER'
# HDIM Marketing Images - Midjourney Prompts

Copy each prompt and paste into Midjourney Discord or web interface.

## Quick Reference
- Use `/imagine prompt:` followed by the prompt
- All prompts include `--v 6.1 --style raw --q 2` parameters
- Aspect ratios are pre-configured per image type

---

HEADER

    for id in "${!PROMPTS[@]}"; do
        local category="${CATEGORIES[$id]}"
        local name="${NAMES[$id]}"
        local dims="${DIMENSIONS[$id]}"
        local prompt="${PROMPTS[$id]}"

        # Determine aspect ratio
        local ar="1:1"
        if [[ "$dims" == "1200x628" || "$dims" == "1920x1080" ]]; then
            ar="16:9"
        fi

        cat >> "$output_file" << EOF
## ${id}: ${name}

**Category:** ${category}
**Dimensions:** ${dims}
**Aspect Ratio:** ${ar}

\`\`\`
/imagine prompt: ${prompt} --ar ${ar} --v 6.1 --style raw --q 2
\`\`\`

---

EOF
    done

    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "Exported ${#PROMPTS[@]} Midjourney prompts"
    echo "File: $output_file"
    echo "════════════════════════════════════════════════════════════════"
}

export_stable_diffusion() {
    local output_json="${OUTPUT_DIR}/exports/sd_prompts_${TIMESTAMP}.json"
    local output_txt="${OUTPUT_DIR}/exports/sd_prompts_${TIMESTAMP}.txt"

    # Create JSON export
    echo "{" > "$output_json"
    local first=true

    for id in "${!PROMPTS[@]}"; do
        local category="${CATEGORIES[$id]}"
        local name="${NAMES[$id]}"
        local dims="${DIMENSIONS[$id]}"
        local prompt="${PROMPTS[$id]}"

        # Determine dimensions for SD
        local width=1024
        local height=1024
        if [[ "$dims" == "1200x628" ]]; then
            width=1024; height=576
        elif [[ "$dims" == "1920x1080" ]]; then
            width=1024; height=576
        fi

        [[ "$first" != "true" ]] && echo "," >> "$output_json"
        first=false

        # Escape quotes for JSON
        local escaped_prompt="${prompt//\\/\\\\}"
        escaped_prompt="${escaped_prompt//\"/\\\"}"

        cat >> "$output_json" << EOF
  "${id}": {
    "name": "${name}",
    "category": "${category}",
    "prompt": "${escaped_prompt}",
    "negative_prompt": "cartoon, anime, illustration, low quality, blurry, text, watermark, logo, signature, deformed, ugly, bad anatomy",
    "width": ${width},
    "height": ${height},
    "steps": 50,
    "cfg_scale": 7.5,
    "sampler": "DPM++ 2M Karras"
  }
EOF
    done

    echo "" >> "$output_json"
    echo "}" >> "$output_json"

    # Create text version
    cat > "$output_txt" << 'HEADER'
# HDIM Marketing Images - Stable Diffusion Prompts

Use these prompts with Stable Diffusion 3, SDXL, or compatible models.

Recommended Settings:
- Steps: 50
- CFG Scale: 7.5
- Sampler: DPM++ 2M Karras

---

HEADER

    for id in "${!PROMPTS[@]}"; do
        local category="${CATEGORIES[$id]}"
        local name="${NAMES[$id]}"
        local dims="${DIMENSIONS[$id]}"
        local prompt="${PROMPTS[$id]}"

        local width=1024
        local height=1024
        if [[ "$dims" == "1200x628" ]]; then
            width=1024; height=576
        elif [[ "$dims" == "1920x1080" ]]; then
            width=1024; height=576
        fi

        cat >> "$output_txt" << EOF
============================================================
${id}: ${name}
============================================================
Category: ${category}

PROMPT:
${prompt}

NEGATIVE PROMPT:
cartoon, anime, illustration, low quality, blurry, text, watermark, logo, signature, deformed, ugly, bad anatomy

SETTINGS:
- Size: ${width}x${height}
- Steps: 50
- CFG Scale: 7.5
- Sampler: DPM++ 2M Karras


EOF
    done

    echo ""
    echo "════════════════════════════════════════════════════════════════"
    echo "Exported ${#PROMPTS[@]} Stable Diffusion prompts"
    echo "JSON: $output_json"
    echo "TXT:  $output_txt"
    echo "════════════════════════════════════════════════════════════════"
}

export_all() {
    export_midjourney
    export_stable_diffusion

    # Also export raw prompts JSON
    local output_file="${OUTPUT_DIR}/exports/all_prompts_${TIMESTAMP}.json"

    echo "{" > "$output_file"
    echo "  \"metadata\": {" >> "$output_file"
    echo "    \"brand\": {" >> "$output_file"
    echo "      \"primary\": \"${PRIMARY}\"," >> "$output_file"
    echo "      \"secondary\": \"${SECONDARY}\"," >> "$output_file"
    echo "      \"success\": \"${SUCCESS}\"," >> "$output_file"
    echo "      \"warning\": \"${WARNING}\"" >> "$output_file"
    echo "    }," >> "$output_file"
    echo "    \"generated\": \"${TIMESTAMP}\"," >> "$output_file"
    echo "    \"prompt_count\": ${#PROMPTS[@]}" >> "$output_file"
    echo "  }," >> "$output_file"
    echo "  \"prompts\": {" >> "$output_file"

    local first=true
    for id in "${!PROMPTS[@]}"; do
        [[ "$first" != "true" ]] && echo "    }," >> "$output_file"
        first=false

        local escaped_prompt="${PROMPTS[$id]//\\/\\\\}"
        escaped_prompt="${escaped_prompt//\"/\\\"}"

        cat >> "$output_file" << EOF
    "${id}": {
      "name": "${NAMES[$id]}",
      "category": "${CATEGORIES[$id]}",
      "dimensions": "${DIMENSIONS[$id]}",
      "prompt": "${escaped_prompt}"
EOF
    done

    echo "    }" >> "$output_file"
    echo "  }" >> "$output_file"
    echo "}" >> "$output_file"

    echo ""
    echo "Master export: $output_file"
}

# ============================================================================
# MAIN
# ============================================================================

main() {
    local platform="gemini"
    local model="gemini-2.0-flash-exp-image-generation"
    local action=""
    local target=""

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --platform)
                platform="$2"
                shift 2
                ;;
            --model)
                model="$2"
                shift 2
                ;;
            --all)
                action="all"
                shift
                ;;
            --category)
                action="category"
                target="$2"
                shift 2
                ;;
            --single)
                action="single"
                target="$2"
                shift 2
                ;;
            --export-midjourney)
                action="export-mj"
                shift
                ;;
            --export-sd)
                action="export-sd"
                shift
                ;;
            --export)
                action="export-all"
                shift
                ;;
            --list)
                action="list"
                shift
                ;;
            --help|-h)
                print_usage
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                echo "Use --help for usage information"
                exit 1
                ;;
        esac
    done

    # Setup directories
    setup_dirs

    # Execute action
    case $action in
        list)
            list_prompts
            ;;
        export-mj)
            export_midjourney
            ;;
        export-sd)
            export_stable_diffusion
            ;;
        export-all)
            export_all
            ;;
        all)
            generate_all "$platform" "$model"
            echo ""
            echo "════════════════════════════════════════════════════════════════"
            echo "Generation complete! Images saved to: ${OUTPUT_DIR}"
            echo "════════════════════════════════════════════════════════════════"
            ;;
        category)
            generate_category "$target" "$platform" "$model"
            echo ""
            echo "════════════════════════════════════════════════════════════════"
            echo "Category '$target' complete! Images saved to: ${OUTPUT_DIR}/${target}"
            echo "════════════════════════════════════════════════════════════════"
            ;;
        single)
            if [[ "$platform" == "gemini" ]]; then
                generate_with_gemini "$target" "$model"
            elif [[ "$platform" == "dalle" ]]; then
                generate_with_dalle "$target"
            fi
            ;;
        *)
            print_usage
            ;;
    esac
}

main "$@"
