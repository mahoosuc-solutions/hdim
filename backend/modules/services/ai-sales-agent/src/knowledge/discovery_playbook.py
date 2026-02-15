"""
30-Minute Discovery Call Playbook

Extracted from: /.claude/skills/sales-discovery-coach.md
Purpose: Provide structured discovery call script for autonomous agent execution.
"""

DISCOVERY_CALL_STRUCTURE = {
    "opening": {
        "duration_minutes": 5,
        "goal": "Build credibility, hook attention, establish rapport",
        "key_messages": [
            "HDIM predicts care gaps 30-60 days ahead using AI",
            "Enables proactive gap closure vs. reactive",
            "Observable SLO contracts with real-time Jaeger visibility",
        ],
        "hook_options": [
            "Most health plans discover gaps reactive—after submission deadlines. What if you could find them 30-60 days ahead?",
            "Your coordinators spend hours manually prioritizing gaps. What if AI showed you the highest-ROI gaps first?",
            "You're leaving money on the table by discovering gaps late. Let me show you what proactive looks like.",
        ],
        "credibility_builders": [
            "We work with [X health plans] managing [Y members]",
            "Our customers capture an average of [$X] additional quality bonus by moving to predictive",
            "Observable SLOs mean you see our performance in real-time via Jaeger—full transparency",
        ],
        "rapport_building": [
            "How long have you been in your role?",
            "What brought you to [Company]?",
            "What's your background before quality?",
        ],
    },
    "pain_discovery": {
        "duration_minutes": 15,
        "goal": "Uncover 3+ specific pain points that HDIM solves",
        "phases": {
            "phase_1": {
                "name": "Current State",
                "questions": [
                    "Tell me about your quality program today—how do you discover gaps?",
                    "What's your current gap closure rate?",
                    "Walk me through your process from gap detection to closure",
                    "How do your coordinators prioritize which gaps to tackle first?",
                ],
                "listen_for": [
                    "Manual workflows (spreadsheets, reports)",
                    "Late gap discovery (after HEDIS submissions)",
                    "Low provider engagement on outreach",
                    "Coordinator overwhelm (not enough time/people)",
                    "No visibility into gap ROI",
                ],
            },
            "phase_2": {
                "name": "HEDIS/Financial Impact",
                "questions": [
                    "How much of your revenue comes from quality bonuses?",
                    "What's your target HEDIS performance this year?",
                    "How many gaps are you currently missing before submission deadlines?",
                    "What would an extra $500K in quality bonus mean to your organization?",
                ],
                "listen_for": [
                    "Specific measure targets (e.g., 'We're targeting HBA1C at 85%')",
                    "Financial impact awareness (quality bonuses = X% of revenue)",
                    "HEDIS deadline pressure (October 31 submission)",
                    "Competitive pressure (other plans ahead on measures)",
                ],
            },
            "phase_3": {
                "name": "Technology & Operations",
                "questions": [
                    "What EHR/claims systems are you using?",
                    "How do you currently validate gap data?",
                    "What tools are you using for gap tracking and outreach?",
                    "How's provider engagement on your gap closure campaigns?",
                ],
                "listen_for": [
                    "Technology stack (Epic, Cerner, Optum, custom data warehouse)",
                    "Data quality concerns",
                    "Provider engagement challenges",
                    "Integration pain points",
                    "Existing vendor dissatisfaction",
                ],
            },
        },
        "qualification_gates": {
            "financial_impact": {
                "question": "If we could help you close an additional 5% of gaps, what would that be worth?",
                "green": "Answers with specific dollar amount ($100K+)",
                "yellow": "Uncertain or estimates low ($50-100K)",
                "red": "Doesn't know or not interested",
            },
            "pain_level": {
                "question": "On a scale of 1-10, how much of your coordinators' time is spent on manual gap management?",
                "green": "8-10 (high pain, motivated to automate)",
                "yellow": "5-7 (moderate pain)",
                "red": "<5 (low pain, maybe not a fit)",
            },
            "timeline": {
                "question": "If you found the right solution, when would you want to implement?",
                "green": "Q1 or Q2 2026 (soon)",
                "yellow": "Q3 2026 (considering but not urgent)",
                "red": "2027 or beyond (not motivated)",
            },
        },
    },
    "solution_positioning": {
        "duration_minutes": 10,
        "goal": "Connect HDIM's capabilities to their pain",
        "approach": "NEVER pitch features first. Always: [Pain] → [HDIM Capability] → [Impact]",
        "framework": {
            "pain_to_capability": [
                {
                    "pain": "Reactive gap discovery (finding gaps after submission windows close)",
                    "capability": "Predictive AI detects gaps 30-60 days ahead using patient risk factors",
                    "impact": "Extra 30-60 days to close gaps = 10-25% more gap closure before deadlines",
                },
                {
                    "pain": "Manual gap prioritization (coordinators don't know which gaps matter most)",
                    "capability": "AI ranks gaps by ROI—which have highest quality bonus impact",
                    "impact": "Coordinators focus on highest-value gaps first = $500K+ in additional bonus capture",
                },
                {
                    "pain": "Low provider engagement on gap closure",
                    "capability": "AI-generated clinical narratives (why this gap matters, recommended actions)",
                    "impact": "Providers get context, not just alerts = 30-50% improvement in engagement",
                },
                {
                    "pain": "No visibility into financial impact of gaps",
                    "capability": "Real-time ROI tracking dashboard (quality bonus impact by measure, provider, gap)",
                    "impact": "Board visibility into quality program ROI + faster decision-making",
                },
                {
                    "pain": "Staffing/budget constraints on quality team",
                    "capability": "Automation replaces manual gap prioritization and tracking",
                    "impact": "Redeploy coordinators to higher-value work (provider relationships, education)",
                },
            ]
        },
        "key_talking_points": [
            "Observable SLOs: You see our performance in real-time via Jaeger dashboard—we're transparent about our reliability",
            "Pilot approach: We recommend starting with a pilot (30-60 days) to prove ROI before full deployment",
            "Implementation speed: 2-4 weeks, not 6+ months",
            "Scalability: Pricing scales with your member population, no expensive infrastructure required",
        ],
    },
    "next_steps": {
        "duration_minutes": 5,
        "goal": "Qualify opportunity and schedule next action",
        "qualification_scoring": {
            "pain_discovery": {"max_points": 20, "criteria": "Did you uncover 3+ specific pain points?"},
            "qualification": {"max_points": 20, "criteria": "Green/yellow/red correct? Min members 25K? Budget 2026?"},
            "objection_handling": {"max_points": 15, "criteria": "Addressed concerns? Built conviction?"},
            "next_steps_clarity": {"max_points": 20, "criteria": "Clear action scheduled? Timeline agreed?"},
            "credibility": {"max_points": 25, "criteria": "Built rapport? Trusted your expertise?"},
        },
        "qualification_framework": {
            "green": {
                "description": "Highly qualified opportunity",
                "criteria": [
                    "25K+ members",
                    "Budget available in Q1 or Q2 2026",
                    "Clear decision timeline (30-60 days)",
                    "High pain level (7+/10)",
                    "Specific HEDIS targets or financial pressure",
                ],
                "next_action": "Schedule 30-minute product demo with CMO + Coordinator",
                "timeline": "Within 7 days",
            },
            "yellow": {
                "description": "Qualified but may need nurturing",
                "criteria": [
                    "20-25K members (borderline)",
                    "Budget timing uncertain (Q3 2026?)",
                    "Decision timeline 60-90 days",
                    "Moderate pain level (5-7/10)",
                    "Interested but not urgent",
                ],
                "next_action": "Send case study + reference call with similar organization",
                "timeline": "14-21 days",
            },
            "red": {
                "description": "Not currently qualified",
                "criteria": [
                    "<20K members (too small)",
                    "No budget until 2027",
                    "Low pain level (<5/10)",
                    "Already committed to competing solution",
                    "Not engaged during discovery",
                ],
                "next_action": "Add to nurture list, re-engage in Q3 2026",
                "timeline": "Follow up in 90 days",
            },
        },
        "next_steps_scripts": {
            "green": "Based on our conversation, this looks like a great fit. I'd love to show you a live demo focused on [pain point they mentioned]. Would next Thursday or Friday work for a 30-minute demo with you and [Coordinator]?",
            "yellow": "You've got the right foundation for HDIM—especially around [pain point]. I'd like to connect you with [similar customer] so you can hear their story. Would an intro call work in the next 2 weeks?",
            "red": "I appreciate the time today. It sounds like timing might not be right in 2026, but I'd love to reconnect in Q3 when [condition they mentioned] changes. Can I add you to quarterly updates?",
        },
        "demo_scheduling_script": "Perfect. Let's get you a demo. I want to focus on [their specific pain point]. Which works better—Thursday Feb 20 at 10am Pacific, or Friday Feb 21 at 2pm? I'll include [Coordinator name] if you want them to join.",
        "crm_logging": {
            "required_fields": [
                "customer_name",
                "contact_name",
                "title",
                "email",
                "phone",
                "company_size (members)",
                "current_pain_points",
                "decision_timeline",
                "budget_range",
                "qualification",
                "next_steps",
                "next_steps_date",
            ],
            "call_transcript": "Full transcript stored for QA and human review",
            "scoring_summary": "Qualification score (0-100) for ranking pipeline",
        },
    },
}

# Specific talking points by persona
PERSONA_TALKING_POINTS = {
    "cmo": {
        "opening_hook": "Most CMOs discover gaps reactively—after submission deadlines. What if we could find them 30-60 days ahead?",
        "pain_questions": [
            "What's your biggest challenge with HEDIS targets this year?",
            "How much time does your team spend prepping for submission?",
        ],
        "solution_focus": "Predictive detection + Real-time ROI tracking = Hit more targets",
        "objection_handling": "Focus on specific measure impact and timeline-to-impact",
    },
    "coordinator": {
        "opening_hook": "Coordinators spend an average of 20% of their time on manual gap prioritization. What if AI showed you the highest-ROI gaps first?",
        "pain_questions": [
            "Walk me through how you currently prioritize gaps",
            "How much time could you save with better automation?",
        ],
        "solution_focus": "Smarter prioritization + Provider engagement improvement = More gaps closed",
        "objection_handling": "Focus on workflow integration and time savings",
    },
    "cfo": {
        "opening_hook": "Your coordinators cost $180K+ annually just for manual gap management. What if AI could reduce that by 50%?",
        "pain_questions": [
            "What's your current investment in quality program infrastructure?",
            "How much in quality bonuses are you leaving on the table?",
        ],
        "solution_focus": "ROI quantification + Cost reduction + Bonus capture",
        "objection_handling": "Focus on financial metrics and competitive benchmarking",
    },
    "it": {
        "opening_hook": "How integrated are your current quality tools with your data warehouse? Most plans run separate systems.",
        "pain_questions": [
            "What integration challenges have you faced with previous healthcare vendors?",
            "How important is HIPAA/HITRUST compliance in your evaluation?",
        ],
        "solution_focus": "Pre-built integrations + Security/compliance + Scalability",
        "objection_handling": "Focus on technical specs, security, and support",
    },
    "provider": {
        "opening_hook": "Providers receive an average of 50+ gap alerts monthly. Most are noise. What if they were high-confidence predictions with clinical context?",
        "pain_questions": [
            "What's your biggest frustration with gap closure alerts today?",
            "How would clinical narratives change your response to alerts?",
        ],
        "solution_focus": "Reduced alert fatigue + Clinical context + Better patient outcomes",
        "objection_handling": "Focus on clinical evidence and alert accuracy",
    },
}

# Observable SLO talking points (referenced in discovery calls)
OBSERVABLE_SLO_INTRO = """
One thing we're really proud of is transparency. Most vendors claim they're 99.9% uptime.
We don't just claim it—we show it to you in real-time via a Jaeger dashboard.

You can see:
- Actual response times (should be <500ms)
- Error rates (should be <0.1%)
- API availability (should be 99.9%+)

This means if we have an issue, you see it immediately. No surprises.
And if we breach our SLOs, you get credits automatically—no negotiation.

That's what we mean by Observable SLOs.
"""
