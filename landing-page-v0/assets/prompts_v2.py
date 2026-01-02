"""V2 Asset Prompts - Healthcare IT Implementation-Informed

These prompts are informed by:
- Real healthcare IT integration patterns (Epic, Cerner, Meditech, athenahealth)
- CMS regulatory requirements and compliance timelines
- Healthcare persona workflows and pain points from stakeholder interviews
- Visual examples from actual healthcare IT implementations

Version: 2.0
Created: January 1, 2025
"""

# Brand Guidelines
BRAND_COLORS = {
    "primary": "#0066CC",     # Deep Blue
    "accent": "#00A5B5",      # Warm Teal
    "background": "#F8FAFC",  # Light Gray
}

# V2 Tier 1 - High Priority Assets
TIER1_PROMPTS = {
    "HERO-V2-MAIN": """Create a professional healthcare technology hero image showing the REAL daily experience of care coordination.

SCENE COMPOSITION:
LEFT SIDE (40%): A care manager (female, 40s, diverse background) at a modern workstation with dual monitors. One monitor shows a unified dashboard with patient care gaps clearly visible - NOT cluttered Excel spreadsheets or 10 different system windows.

RIGHT SIDE (40%): A warm, semi-transparent overlay showing the RESULT - a grandmother (67, Latina) healthy and playing with grandchildren in a garden setting.

CENTER CONNECTION (20%): Subtle flowing data visualization (blue to teal gradient lines) connecting the technology to the human outcome.

TECHNICAL ACCURACY - Dashboard Elements Must Include:
- Patient care gap list with severity indicators (red/yellow/green)
- Star Rating progress meter showing improvement
- Multi-tenant selector dropdown (suggests enterprise capability)

MOOD: The contrast between the clean, modern technology on the left and the warm human outcome on the right tells the story: "Technology enables humanity."

AVOID: Generic stock photography feeling, cluttered interfaces, cold/clinical aesthetics, unrealistic data screens.

Technical requirements:
- Photorealistic quality
- Dashboard text must be legible at 1920x1080
- No text errors in UI elements
- Brand colors prominent: Deep blue #0066CC, Teal #00A5B5""",

    "DASHBOARD-CARE-GAP": """Create a realistic, modern healthcare quality dashboard that HDIM actually delivers.

LAYOUT STRUCTURE:

HEADER BAR:
- Healthcare platform logo (left)
- Tenant/Organization selector: "Midwest Health Plan" dropdown
- User menu (right): "Sarah Torres, RN" with avatar
- Last sync: "Real-time | 2 min ago"

LEFT SIDEBAR (Navigation):
- Dashboard (active)
- Care Gaps
- Patient Search
- Measures
- Analytics
- Settings

MAIN CONTENT AREA:

TOP ROW - Key Metrics (4 cards):
1. "Open Gaps" - 2,847 (down arrow -12% from last month)
2. "Gaps Closed Today" - 47 (sparkline showing daily trend)
3. "Star Rating Projection" - 4.0 Stars (up from 3.5)
4. "Outreach Queue" - 156 members prioritized

MIDDLE ROW - Priority Worklist:
Table showing:
| Patient | Risk Score | Open Gaps | Priority | Actions |
| Johnson, M. | 87 (High) | 4 | Critical | [Call] [View] |
| Williams, R. | 72 (Rising) | 2 | High | [Call] [View] |
| Chen, L. | 45 (Moderate) | 1 | Medium | [Call] [View] |

BOTTOM ROW:
Left: Bar chart showing "Gap Closure Trend - Last 12 Weeks" (upward trend)
Right: Pie chart showing "Gaps by Measure Category" (Preventive, Chronic, Pharmacy)

DESIGN REQUIREMENTS:
- Clean, modern SaaS aesthetic (Stripe/Linear design influence)
- White/light gray backgrounds
- Brand blue #0066CC for primary elements
- Brand teal #00A5B5 for positive trends/highlights
- High contrast for accessibility
- Realistic, functional data (not Lorem ipsum)

TEXT MUST BE READABLE at 1440x900 - no blurry UI elements""",

    "PORTRAIT-SARAH-CAREMGR": """Create an authentic portrait of a care manager in their actual work environment.

SUBJECT: Sarah, 38, experienced care manager. She wears business casual (cardigan over blouse), has a headset around her neck, and looks directly at camera with an expression that says "I've seen everything."

SETTING: Her actual workstation - NOT a sterile stock photo office. Include:
- Personal photo of her kids on desk
- Coffee mug with healthcare-related humor
- Dual monitors visible (slightly blurred) showing patient lists
- Notepad with hand-written notes
- Small plant adding life to space

EXPRESSION: Warm but tired eyes that show compassion mixed with the weight of responsibility. A small, genuine smile.

LIGHTING: Warm overhead office lighting with natural light from nearby window.

MOOD: This is the human behind the technology - she cares deeply about her patients but is overwhelmed by the systems.

INCLUDE IN FRAME (for storytelling):
- Her name badge showing "Sarah Torres, RN, CCM"
- A "years of service" pin (8+ years)
- Subtle personal touches that humanize the workplace

PURPOSE: This image represents who the platform is built FOR - the frontline care coordinator who deserves better tools.

Technical: 800x1000, photorealistic, warm office lighting""",

    "PORTRAIT-ELEANOR": """Create a powerful portrait of Eleanor - a colorectal cancer survivor caught early through screening.

SUBJECT: Eleanor, 68, African American woman. She exudes strength and gratitude. Her silver locs are styled elegantly. She wears a deep blue blouse (brand color #0066CC) with a meaningful piece of jewelry.

SETTING: Her living room, comfortable and full of life. Family photos in background show multiple generations. A Bible or meaningful book on side table suggests faith/community.

KEY VISUAL ELEMENTS:
- Photo frame visible showing her with daughter who pushed her to get screened
- A "Survivor" ribbon pin on her lapel (subtle)
- Healthy glow in her skin
- Plants thriving in her space (life, growth)
- Light streaming in creating a hopeful atmosphere

EXPRESSION: Peaceful power. A woman who faced fear, pushed through it, and won. Her gaze is direct and confident. A knowing smile.

MOOD: "I put off that test for 5 years. My daughter kept calling. My care manager didn't give up. That polyp would have killed me."

PURPOSE: This is the human stakes behind every care gap. Eleanor's story is why the technology matters.

Technical: 800x1000, photorealistic, warm natural lighting""",

    "BADGE-FHIR-V2": """Create a modern compliance badge for FHIR R4 certification.

DESIGN:
- Circular badge with subtle depth/shadow
- FHIR fire icon in center (stylized, not literal)
- "R4" prominently displayed
- Ring text: "FHIR R4 NATIVE"
- Brand blue #0066CC primary, white accents
- Clean, minimal, professional

AVOID: Cluttered design, excessive text, dated certificate aesthetic

Technical: 200x200, clean vector style""",

    "BADGE-CQL-V2": """Create a badge representing CQL (Clinical Quality Language) capability.

DESIGN:
- Hexagonal shape (referencing code/logic)
- Code brackets icon: { } or < >
- "CQL" text centered
- Subtle circuit/logic patterns in background
- "NCQA Certified Logic" text around edge
- Brand colors: Blue #0066CC with teal #00A5B5 accent

MEANING: This badge signals "we execute the official measure logic, not our interpretation"

Technical: 200x200, modern icon design""",
}

# V2 Tier 2 - Secondary Priority Assets
TIER2_PROMPTS = {
    "HERO-BEFORE-AFTER": """Create a realistic healthcare IT workflow hero image showing BEFORE vs AFTER.

SPLIT SCREEN COMPOSITION:

LEFT HALF - "WITHOUT" (desaturated, grayer tones):
A stressed care manager surrounded by chaos:
- Multiple sticky notes on monitor edges
- 10+ browser tabs visible (representing system chaos)
- Phone cradled on shoulder while typing
- Excel spreadsheet with highlighted gaps
- Stack of faxed documents
- Clock showing 5:30 PM (overtime)
- Expression: frustrated, overwhelmed

RIGHT HALF - "WITH" (bright, brand colors):
The same care manager, transformed:
- Single unified dashboard with clear information hierarchy
- One screen showing patient context, gaps, and next actions
- Headset on, relaxed posture
- Clock showing 4:00 PM (leaving on time)
- Expression: confident, engaged, smiling
- Small notification: "12 gaps closed today" visible on screen

CENTER DIVIDER: Subtle platform logo as the transformation point

MOOD: The visual contrast makes the value proposition immediately clear without needing to read anything.

Technical requirements: 1920x1080, photorealistic, readable UI elements, brand-aligned colors on right side""",

    "PORTRAIT-MARIA-V2": """Create a warm, genuine portrait of Maria - a diabetes patient whose care gaps were closed.

SUBJECT: Maria, 67, Latina grandmother. She has kind eyes, silver-streaked dark hair pulled back, and wears comfortable everyday clothing - a soft cardigan in warm teal over a cream blouse.

SETTING: Her warm kitchen with family photos on the wall (slightly blurred). Morning light streams through a window with herbs growing on the sill.

KEY VISUAL ELEMENTS (telling her clinical story subtly):
- A small A1C testing device visible on counter (diabetes context)
- Her insulin pen case (organized, not hidden - she's managing well)
- A healthy meal prep visible (fresh vegetables, portion control)
- Calendar on wall with doctor appointments marked (engaged in care)
- Grandchildren's artwork on refrigerator (her motivation)

EXPRESSION: Serene confidence. A woman who was once overwhelmed by her condition but now has it under control. Her smile is grateful and forward-looking.

MOOD: "I almost missed the care that saved my life. Now I'm here for my grandchildren."

AVOID: Medical imagery prominence (she's a person first, patient second), sad or worried expression, institutional settings

Technical: 800x1000, photorealistic, warm morning light""",

    "PORTRAIT-JAMES-V2": """Create a hopeful portrait of James - a patient who recovered from depression through early screening.

SUBJECT: James, 42, professional man. He's outdoors, on a morning walk or at a park bench. Business casual clothing (rolled sleeves, khakis) suggests work-life balance restored.

SETTING: Outdoor park or trail in early morning light. Trees, greenery, and signs of nature prominent. A sense of openness and fresh air.

KEY VISUAL ELEMENTS (telling his recovery story):
- Running shoes (he's active again)
- Wedding ring prominent (reconnected with family)
- Slight smile that reaches his eyes (genuine contentment)
- Phone in pocket showing a notification (he's connected, not isolated)
- A coffee cup from a local shop (engaging with community)

EXPRESSION: Calm, present, grateful. This is a man who came through darkness and found his way back. His eyes show depth - he's been through something, but he's okay now.

MOOD: "PHQ-2 screening caught what I couldn't see myself. Two years ago, I couldn't imagine feeling this way again."

AVOID: Sad or brooding imagery, institutional settings, obvious "mental health" visual cues, before/after dramatic contrast

Technical: 800x1000, photorealistic, warm outdoor morning light""",

    "ARCHITECTURE-INTEGRATION": """Create a modern architecture diagram showing how the platform connects to real healthcare systems.

LAYOUT: Hub-and-spoke with platform at center

CENTER HUB:
Platform hexagon containing:
- "CQL Engine" icon (code brackets)
- "FHIR R4 Core" icon (fire symbol)
- "Care Gap Detection" icon (target)
- "29 Microservices" text

SURROUNDING CONNECTIONS (spokes radiating out):

TOP (EHR Systems):
- Epic logo placeholder -> "FHIR R4 API"
- Cerner/Oracle Health logo -> "Millennium APIs"
- Meditech logo -> "Expanse Integration"
- athenahealth logo -> "athenaOne APIs"

RIGHT (Payer Systems):
- HealthEdge logo -> "Claims Feed"
- TriZetto logo -> "Eligibility API"
- Custom icon -> "834/837 EDI"

BOTTOM (Data Exchanges):
- CommonWell logo -> "Patient Matching"
- Carequality logo -> "National Network"
- "TEFCA Ready" badge

LEFT (Regulatory/Reporting):
- CMS logo -> "QRDA I/III Export"
- NCQA logo -> "HEDIS Certified"
- "Star Ratings" icon

DATA FLOW VISUALIZATION:
- Animated-style arrows showing bidirectional data flow
- Blue arrows for data IN
- Teal arrows for insights OUT
- Small icons on arrows showing data types (patient, measure, gap)

STYLE:
- Clean, modern tech diagram aesthetic
- Not cluttered - breathing room between elements
- Brand colors throughout: #0066CC and #00A5B5
- Subtle grid background suggesting precision

LABEL: "Real-time FHIR R4 integration with major healthcare systems"

Technical: 1400x800, clean vector/diagram style""",

    "BADGE-HIPAA-5MIN": """Create a badge highlighting the 5-minute PHI cache commitment.

DESIGN:
- Shield shape (protection)
- Clock icon showing 5 minutes
- "PHI ≤5min" text
- "HIPAA COMPLIANT" around shield edge
- Lock icon integrated subtly
- Brand blue #0066CC with security/trust feel

MEANING: Differentiator showing stricter-than-required data handling

Technical: 200x200, modern security badge style""",

    "DASHBOARD-MOBILE": """Create a mobile-responsive version of the care gap dashboard.

SCREEN STRUCTURE:

TOP:
- Hamburger menu (left)
- Platform wordmark (center)
- Bell notification icon with badge (right)

KEY METRICS STRIP:
Horizontal scroll of card summaries:
[Open Gaps: 2,847] [Today: +47] [Rating: 4.0*] ->

MAIN CONTENT:
"My Priority List" heading
Swipeable patient cards:

CARD 1:
- Patient avatar (generic, respectful)
- "Margaret J., 68"
- "4 open gaps | High Priority"
- Risk badge: "HCC: 2.45"
- Quick action buttons: [Call] [Directions]

CARD 2:
- Similar structure

BOTTOM NAV:
[Home] [Patients] [Gaps] [Profile]

DESIGN: Native iOS/Android feel, thumb-friendly tap targets, dark mode compatible colors

Technical: 375x812, mobile UI design""",
}

# V2 Tier 3 - Supporting Assets
TIER3_PROMPTS = {
    "HERO-COMMAND-CENTER": """Create a modern healthcare operations center visualization showing population health management at scale.

SCENE: A bright, modern open-plan healthcare IT operations room (NOT a dark NOC).

FOREGROUND:
- 2-3 quality analysts at standing desks with large curved monitors
- Monitors showing population health dashboards with:
  - Geographic heat map of care gaps by ZIP code
  - Real-time Star Rating projection (showing 3.5 -> 4.0 trajectory)
  - Member stratification pyramid (high-risk, rising-risk, stable)
  - HEDIS measure performance gauges

BACKGROUND:
- Large wall display showing aggregate metrics
- "LIVE" indicator suggesting real-time data
- Regional map with health plan coverage area
- Team collaboration visible (2 people discussing a screen)

SUBTLE DETAILS TO INCLUDE:
- FHIR R4 logo visible somewhere on screen (interoperability)
- "Last updated: 2 min ago" timestamp (real-time capability)
- Compliance badge showing "HIPAA Compliant"
- CMS deadline countdown (suggests regulatory awareness)

MOOD: Professional, calm control over complexity, data-driven decision making

AVOID: Dark/dramatic NOC aesthetics, cluttered screens, cold industrial feeling

Technical: 1920x1080, photorealistic, modern office lighting""",

    "CARE-MANAGER-SUCCESS": """Create a portrait showing the MOMENT of impact - a care manager on a call that matters.

SUBJECT: Male care manager, 45, African American, in focused conversation on headset.

SCENE DETAILS:
- He's leaning slightly forward, engaged
- His monitor shows a patient profile with several care gaps
- One gap is highlighted with a "CLOSING" status
- His expression shows genuine engagement - eyes crinkled with empathy
- One hand gestures as if explaining something compassionately

MONITOR VISIBLE (semi-blurred but readable):
- Patient name: "Eleanor W., 68"
- Gap: "Colorectal Cancer Screening - Overdue 5 years"
- Status changing from red to green
- Notes field showing conversation in progress

MOOD: This is the moment of impact - the call that saves a life.

LIGHTING: Warm, office lighting with screen glow adding dimension

PURPOSE: Shows technology enabling human connection, not replacing it

Technical: 800x1000, photorealistic, warm office light""",

    "VIDEO-THUMB-CAREMGR": """Create a video thumbnail showing a care manager's transformation.

COMPOSITION:
- Care manager Sarah in foreground (warm, confident expression)
- Split background: chaotic desk (left, faded) vs clean dashboard (right, prominent)
- Text overlay: "From 10 Systems to 1 Screen"
- Play button overlay (subtle)
- Platform logo corner

MOOD: Professional, relatable, transformation story

Technical: 1920x1080, video thumbnail style""",

    "VIDEO-THUMB-ELEANOR": """Create a video thumbnail for Eleanor's colorectal screening story.

COMPOSITION:
- Eleanor's portrait (warm, grateful expression)
- Soft focus family photos in background
- Quote overlay: "They Caught It Early"
- Subtle medical iconography (screening related)
- Play button overlay
- Platform logo corner

MOOD: Emotional, human, hope

Technical: 1920x1080, video thumbnail style""",

    "SOCIAL-LINKEDIN": """Create a LinkedIn post template for healthcare IT thought leadership.

LAYOUT:
- Left 60%: Space for headline text
- Right 40%: Relevant imagery (dashboard, person, or icon)
- Bottom: Platform logo and CTA

COLOR SCHEME:
- Deep blue #0066CC gradient background
- White text for headlines
- Teal #00A5B5 accent for highlights

SAMPLE HEADLINE OVERLAYS:
- "56 HEDIS Measures. One Platform."
- "Real-Time Beats Retroactive."
- "Your Star Rating Depends on Data Speed."

Technical: 1200x627, social media template""",

    "SOCIAL-TWITTER-STATS": """Create a Twitter card template for sharing statistics.

LAYOUT:
- Large statistic prominently displayed (e.g., "99.7%")
- Supporting text below (e.g., "reduction in PHI exposure window")
- Platform branding
- Clean, shareable design

COLOR: Brand blue #0066CC background, white text, teal accent for statistic

Technical: 1200x675, Twitter card format""",

    "BADGE-STAR-RATINGS": """Create a badge showing Star Rating improvement capability.

DESIGN:
- Star shape (obvious but elevated design)
- Arrow pointing up through star
- "3.5 -> 4.5" transformation text
- "CMS Star Ratings" around edge
- Gold/teal gradient for premium feel

MEANING: Shows measurable quality improvement outcome

Technical: 200x200, premium badge style""",

    "HIPAA-5MIN-STORY": """Create a powerful visual explaining the 5-minute PHI cache TTL commitment.

SPLIT COMPOSITION:

LEFT SIDE (60%):
Technical visualization showing:
- Timeline from 0 to 5 minutes
- Data flow: "PHI Accessed" -> "Processing" -> "Purged at 5:00"
- Contrast callout: "Industry standard: 2-6 hours"
- Visual showing 99.7% reduction in exposure window

RIGHT SIDE (40%):
Human impact:
- Abstract representation of patient trust (hands, protection imagery)
- Quote overlay: "Your data is only here when it needs to be"
- HIPAA shield badge

CENTER:
The actual code commit that made this decision:
fix(hipaa): Reduce PHI cache TTL to ≤5min
December 27, 2025

MOOD: Technical precision meets patient protection. The code tells the story of a decision made for the right reasons.

DESIGN: Dark code editor aesthetic on left transitioning to warm human imagery on right

Technical: 1920x1080, mixed tech/human style""",
}

# Combined prompt dictionary for easy access
ALL_V2_PROMPTS = {
    **TIER1_PROMPTS,
    **TIER2_PROMPTS,
    **TIER3_PROMPTS,
}

# V2 Asset Specifications (to add to config.py)
V2_IMAGE_SPECS = {
    "hero-v2": {
        "HERO-V2-MAIN": {"width": 1920, "height": 1080, "name": "Main Hero - Care Manager + Outcome"},
        "HERO-BEFORE-AFTER": {"width": 1920, "height": 1080, "name": "Before/After Transformation"},
        "HERO-COMMAND-CENTER": {"width": 1920, "height": 1080, "name": "Population Health Command Center"},
    },
    "dashboards-v2": {
        "DASHBOARD-CARE-GAP": {"width": 1440, "height": 900, "name": "Care Gap Dashboard - Realistic"},
        "DASHBOARD-MOBILE": {"width": 375, "height": 812, "name": "Mobile Dashboard View"},
    },
    "portraits-v2": {
        "PORTRAIT-SARAH-CAREMGR": {"width": 800, "height": 1000, "name": "Sarah - Care Manager Portrait"},
        "PORTRAIT-ELEANOR": {"width": 800, "height": 1000, "name": "Eleanor - Cancer Survivor"},
        "PORTRAIT-MARIA-V2": {"width": 800, "height": 1000, "name": "Maria - Diabetes (Clinical Context)"},
        "PORTRAIT-JAMES-V2": {"width": 800, "height": 1000, "name": "James - Depression Recovery"},
        "CARE-MANAGER-SUCCESS": {"width": 800, "height": 1000, "name": "Care Manager - Moment of Impact"},
    },
    "badges-v2": {
        "BADGE-FHIR-V2": {"width": 200, "height": 200, "name": "FHIR R4 Native Badge"},
        "BADGE-CQL-V2": {"width": 200, "height": 200, "name": "CQL Standards Badge"},
        "BADGE-HIPAA-5MIN": {"width": 200, "height": 200, "name": "HIPAA 5-Minute Cache Badge"},
        "BADGE-STAR-RATINGS": {"width": 200, "height": 200, "name": "Star Ratings Improvement"},
    },
    "architecture-v2": {
        "ARCHITECTURE-INTEGRATION": {"width": 1400, "height": 800, "name": "Healthcare IT Integration Diagram"},
        "HIPAA-5MIN-STORY": {"width": 1920, "height": 1080, "name": "5-Minute HIPAA Story Visual"},
    },
    "video-v2": {
        "VIDEO-THUMB-CAREMGR": {"width": 1920, "height": 1080, "name": "Care Manager Video Thumbnail"},
        "VIDEO-THUMB-ELEANOR": {"width": 1920, "height": 1080, "name": "Eleanor Story Thumbnail"},
    },
    "social-v2": {
        "SOCIAL-LINKEDIN": {"width": 1200, "height": 627, "name": "LinkedIn Post Template"},
        "SOCIAL-TWITTER-STATS": {"width": 1200, "height": 675, "name": "Twitter Statistics Card"},
    },
}

# V2 Generation Priority Order
V2_PRIORITY = [
    # Tier 1 - Week 1
    "HERO-V2-MAIN",
    "DASHBOARD-CARE-GAP",
    "PORTRAIT-SARAH-CAREMGR",
    "PORTRAIT-ELEANOR",
    "BADGE-FHIR-V2",
    "BADGE-CQL-V2",
    # Tier 2 - Week 2
    "HERO-BEFORE-AFTER",
    "PORTRAIT-MARIA-V2",
    "PORTRAIT-JAMES-V2",
    "ARCHITECTURE-INTEGRATION",
    "BADGE-HIPAA-5MIN",
    "DASHBOARD-MOBILE",
    # Tier 3 - Weeks 3-4
    "HERO-COMMAND-CENTER",
    "CARE-MANAGER-SUCCESS",
    "VIDEO-THUMB-CAREMGR",
    "VIDEO-THUMB-ELEANOR",
    "SOCIAL-LINKEDIN",
    "SOCIAL-TWITTER-STATS",
    "BADGE-STAR-RATINGS",
    "HIPAA-5MIN-STORY",
]


def get_v2_prompt(asset_id: str) -> str:
    """Get the V2 prompt for an asset ID."""
    return ALL_V2_PROMPTS.get(asset_id, "")


def get_v2_asset_category(asset_id: str) -> str:
    """Get the V2 category for an asset ID."""
    for category, assets in V2_IMAGE_SPECS.items():
        if asset_id in assets:
            return category
    return "unknown"


def get_v2_asset_spec(asset_id: str) -> dict:
    """Get the V2 specification for an asset ID."""
    for category, assets in V2_IMAGE_SPECS.items():
        if asset_id in assets:
            return assets[asset_id]
    return {}


def get_tier1_assets() -> list:
    """Get list of Tier 1 (highest priority) asset IDs."""
    return V2_PRIORITY[:6]


def get_tier2_assets() -> list:
    """Get list of Tier 2 asset IDs."""
    return V2_PRIORITY[6:12]


def get_tier3_assets() -> list:
    """Get list of Tier 3 asset IDs."""
    return V2_PRIORITY[12:]
