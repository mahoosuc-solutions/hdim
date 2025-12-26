#!/bin/bash
# People Helping People - Human-Centered Marketing Images
# Focus: Providers using HDIM to help patients, answer hard questions, access insights

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="${SCRIPT_DIR}/generated-images/people"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# API Key
GOOGLE_API_KEY="${GOOGLE_API_KEY:-AIzaSyBJKY_Hml7wvwxdppZQjET_imtwnAELhck}"

mkdir -p "$OUTPUT_DIR"

# Prompts for people-helping-people images
declare -A PROMPTS
declare -A NAMES

NAMES["PEOPLE-01"]="Doctor-Patient Consultation with Insights"
PROMPTS["PEOPLE-01"]="Create a warm, professional 4K image of a caring physician (female, 40s, white coat) sitting with an elderly patient in a modern clinic exam room. The doctor is showing the patient information on a tablet, pointing to a clear data visualization. The patient looks relieved and engaged. A large monitor behind them shows a clean, organized patient dashboard with green indicators. Soft natural light from a window. Mood: Trust, care, empowerment, understanding. The technology enables the human connection, not replaces it. Style: Editorial healthcare photography, warm tones, authentic emotion. Colors: Soft whites, warm wood tones, teal accent from screen. Composition: Doctor and patient as equals, technology as enabler."

NAMES["PEOPLE-02"]="Care Team Collaboration"
PROMPTS["PEOPLE-02"]="Create a 4K image of a diverse healthcare care team (doctor, nurse, care coordinator) huddled around a large screen in a modern hospital conference room. They are collaboratively reviewing a patient case, with the screen showing unified patient data, care gaps highlighted in amber, and recommended actions. Their body language shows engagement and purpose - pointing at screen, nodding, one taking notes. Mood: Teamwork, collaboration, shared mission to help patients. Style: Authentic corporate healthcare photography. Natural lighting. Colors: Clean whites, navy scrubs, teal UI accents on screen. Composition: Team centered, screen visible, showing unified data solving a complex case."

NAMES["PEOPLE-03"]="Nurse Accessing Critical Information"
PROMPTS["PEOPLE-03"]="Create an authentic 4K image of a registered nurse (male, 30s, scrubs) at a hospital bedside, using a tablet to quickly access patient information. The patient (elderly woman) is resting comfortably in the bed. The nurse's expression shows confidence and focus - he has the information he needs instantly. The tablet screen shows a clean patient summary with vital history. Mood: Competence, care, efficiency, trust. The nurse can focus on the patient because the technology works. Style: Documentary healthcare photography, warm hospital lighting. Colors: Blue scrubs, warm ambient light, clean white sheets. Composition: Nurse as hero, patient comfortable, technology unobtrusive."

NAMES["PEOPLE-04"]="Provider Answering Family Questions"
PROMPTS["PEOPLE-04"]="Create a compassionate 4K image of a physician (male, 50s, white coat) sitting in a family consultation room with concerned adult children (man and woman, 40s) of an elderly patient. The doctor has a tablet showing clear visualizations of their parent's health journey and care plan. The family members look relieved as the doctor explains with confidence. Mood: Compassion, clarity, reassurance, trust. The doctor has answers because the data is unified. Style: Warm editorial healthcare photography. Soft lighting, comfortable consultation room. Colors: Warm neutrals, soft blues, calm environment. Composition: Intimate conversation, doctor at eye level with family, tablet as reference tool."

NAMES["PEOPLE-05"]="Population Health Insights Discovery"
PROMPTS["PEOPLE-05"]="Create a 4K image of a Chief Medical Officer (female, 55, professional attire) and a Quality Director (male, 45, business casual) reviewing population health insights on a large wall display in a modern hospital executive office. The display shows care gap analytics, quality measures trending upward, and patient cohorts. They are having an 'aha moment' - discovering insights that were previously hidden. Mood: Discovery, empowerment, strategic thinking, optimism. Style: Modern executive healthcare setting, natural light from windows. Colors: Navy accents, clean whites, teal/green positive indicators on screen. Composition: Two leaders collaborating, screen showing actionable insights, city/campus view through windows."

NAMES["PEOPLE-06"]="Medical Assistant Empowered"
PROMPTS["PEOPLE-06"]="Create an authentic 4K image of a medical assistant (female, late 20s, scrubs) at a check-in workstation, confidently helping a patient (middle-aged man) complete their pre-visit paperwork on a tablet. The workstation monitor behind her shows the patient's unified record already pulled up and ready. She looks confident and efficient, not stressed. The patient looks appreciative. Mood: Empowerment, efficiency, helpfulness, smooth workflow. Even front-line staff have the tools they need. Style: Modern clinic photography, bright and welcoming. Colors: Cheerful clinic environment, teal accent colors. Composition: MA as the hero, patient engaged, technology seamlessly integrated."

NAMES["PEOPLE-07"]="After-Hours Care Coordination"
PROMPTS["PEOPLE-07"]="Create a 4K image of a care coordinator (female, 35, business casual) working from a home office in the evening, on a video call with an elderly patient visible on her monitor. Her second screen shows the patient's complete health record and care plan. She's smiling warmly while explaining something. The home setting is professional but comfortable - evening light, cup of tea nearby. Mood: Dedication, connection, continuity of care, work-life integration. Care doesn't stop at 5pm. Style: Remote work lifestyle photography, warm evening lighting. Colors: Warm home tones, cool screen light, professional setup. Composition: Coordinator as caring professional, patient visible on video, data accessible."

NAMES["PEOPLE-08"]="Specialist Consultation Made Easy"
PROMPTS["PEOPLE-08"]="Create a 4K image of a specialist physician (cardiologist, female, 45, white coat) reviewing a referred patient's complete history on a large monitor before a consultation. She looks satisfied that she has everything she needs - no missing records, no phone calls to track down information. The screen shows a comprehensive view: prior imaging, lab trends, PCP notes, medications. Mood: Preparedness, confidence, efficiency, quality care. Specialists get the full picture before the patient arrives. Style: Modern specialty clinic, professional lighting. Colors: Clean medical environment, organized data visualization. Composition: Specialist reviewing complete information, ready to provide excellent care."

echo "Generating people-helping-people images..."

for id in "${!PROMPTS[@]}"; do
    name="${NAMES[$id]}"
    prompt="${PROMPTS[$id]}"
    output_file="${OUTPUT_DIR}/${id}_${TIMESTAMP}.png"

    echo ""
    echo ">>> Generating: $id - $name"

    # Escape prompt for JSON
    escaped_prompt="${prompt//\\/\\\\}"
    escaped_prompt="${escaped_prompt//\"/\\\"}"
    escaped_prompt="${escaped_prompt//$'\n'/\\n}"

    response=$(curl -s "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp-image-generation:generateContent?key=${GOOGLE_API_KEY}" \
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

    echo "$response" > "${output_file%.png}_response.json"

    if echo "$response" | grep -q '"error"'; then
        echo "    API Error - trying gemini-2.5-flash-image model..."

        response=$(curl -s "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=${GOOGLE_API_KEY}" \
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

        echo "$response" > "${output_file%.png}_response.json"
    fi

    image_data=$(python3 -c "
import json
try:
    data = json.load(open('${output_file%.png}_response.json'))
    candidates = data.get('candidates', [])
    if candidates:
        parts = candidates[0].get('content', {}).get('parts', [])
        for part in parts:
            if 'inlineData' in part:
                print(part['inlineData'].get('data', ''))
                break
except:
    pass
" 2>/dev/null)

    if [[ -n "$image_data" && "$image_data" != "" ]]; then
        echo "$image_data" | base64 -d > "$output_file"
        echo "    Success! Saved to: $output_file"
        rm -f "${output_file%.png}_response.json"
    else
        echo "    Warning: Could not extract image"
    fi

    sleep 2  # Rate limiting
done

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "People-Helping-People images generated!"
echo "Output: ${OUTPUT_DIR}"
echo "════════════════════════════════════════════════════════════════"
