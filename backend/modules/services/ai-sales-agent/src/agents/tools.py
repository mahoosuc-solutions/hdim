"""
Shared tool definitions for AI Sales Agent team.

These tools are available to all agent types:
- Coordinator Agent
- Discovery Agent
- Demo Agent
- Objection Agent
- Pipeline Agent
"""

# ============================================================================
# DISCOVERY AGENT TOOLS
# ============================================================================

DISCOVERY_TOOLS = [
    {
        "name": "ask_discovery_question",
        "description": "Ask customer a discovery question from the 30-minute script. Pause for their response.",
        "input_schema": {
            "type": "object",
            "properties": {
                "question": {
                    "type": "string",
                    "description": "The discovery question to ask",
                },
                "question_type": {
                    "type": "string",
                    "enum": ["pain", "qualification", "financial", "technical", "opening", "closing"],
                    "description": "Category of question (affects scoring)",
                },
                "context": {
                    "type": "string",
                    "description": "Brief context (e.g., 'Moving to Phase 2: Pain Discovery')",
                },
            },
            "required": ["question", "question_type"],
        },
    },
    {
        "name": "listen_and_transcribe",
        "description": "Simulate listening to customer response and transcribing their answer.",
        "input_schema": {
            "type": "object",
            "properties": {
                "prompt": {
                    "type": "string",
                    "description": "Instruction for simulated customer response (test only)",
                },
                "listening_for": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Key phrases/signals to detect in response",
                },
            },
            "required": ["listening_for"],
        },
    },
    {
        "name": "score_discovery_dimension",
        "description": "Score one dimension of the discovery call (0-20 scale)",
        "input_schema": {
            "type": "object",
            "properties": {
                "dimension": {
                    "type": "string",
                    "enum": [
                        "pain_discovery",
                        "qualification",
                        "objection_handling",
                        "next_steps",
                        "credibility",
                    ],
                    "description": "Which dimension to score",
                },
                "evidence": {
                    "type": "string",
                    "description": "Evidence from call transcript supporting this score",
                },
                "score": {
                    "type": "integer",
                    "minimum": 0,
                    "maximum": 20,
                    "description": "Score for this dimension",
                },
            },
            "required": ["dimension", "score"],
        },
    },
    {
        "name": "qualify_opportunity",
        "description": "Qualify lead as green/yellow/red based on criteria",
        "input_schema": {
            "type": "object",
            "properties": {
                "qualification": {
                    "type": "string",
                    "enum": ["green", "yellow", "red"],
                    "description": "Qualification level",
                },
                "reasoning": {
                    "type": "string",
                    "description": "Why this qualification? (cite specific criteria met/not met)",
                },
                "member_count": {
                    "type": "integer",
                    "description": "Customer member population size",
                },
                "budget_timeline": {
                    "type": "string",
                    "enum": ["Q1 2026", "Q2 2026", "Q3 2026", "2027+", "unknown"],
                    "description": "When can they budget for solution?",
                },
                "pain_level": {
                    "type": "integer",
                    "minimum": 1,
                    "maximum": 10,
                    "description": "How urgent is their pain? (1=low, 10=critical)",
                },
            },
            "required": ["qualification", "reasoning"],
        },
    },
    {
        "name": "schedule_next_step",
        "description": "Schedule the next action (demo, reference call, nurture)",
        "input_schema": {
            "type": "object",
            "properties": {
                "next_action": {
                    "type": "string",
                    "enum": ["demo", "reference_call", "proposal", "nurture", "escalate"],
                    "description": "What happens next",
                },
                "scheduled_date": {
                    "type": "string",
                    "description": "Date/time for next action (ISO format or relative: 'Feb 20 10am PT')",
                },
                "attendees": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Who should attend (their names/titles)",
                },
                "prep_materials": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Materials to send before next step (case study, demo link, etc.)",
                },
            },
            "required": ["next_action"],
        },
    },
    {
        "name": "update_crm",
        "description": "Log call notes, qualification, next steps to CRM",
        "input_schema": {
            "type": "object",
            "properties": {
                "contact_id": {
                    "type": "string",
                    "description": "Customer contact ID in CRM",
                },
                "call_summary": {
                    "type": "string",
                    "description": "5-10 sentence summary of call",
                },
                "pain_points": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Discovered pain points",
                },
                "qualification": {
                    "type": "string",
                    "enum": ["green", "yellow", "red"],
                },
                "next_steps": {
                    "type": "string",
                    "description": "What happens next",
                },
                "next_steps_date": {
                    "type": "string",
                    "description": "Scheduled date for next step",
                },
                "call_transcript": {
                    "type": "string",
                    "description": "Full call transcript (for QA/human review)",
                },
                "call_score": {
                    "type": "number",
                    "minimum": 0,
                    "maximum": 100,
                    "description": "Overall call quality score (0-100)",
                },
            },
            "required": [
                "contact_id",
                "call_summary",
                "qualification",
                "next_steps",
                "call_score",
            ],
        },
    },
]

# ============================================================================
# DEMO AGENT TOOLS (PLANNED - Week 2)
# ============================================================================

DEMO_TOOLS = [
    {
        "name": "generate_demo_outline",
        "description": "Generate persona-specific demo outline (15/30/45 min duration)",
        "input_schema": {
            "type": "object",
            "properties": {
                "persona": {
                    "type": "string",
                    "enum": ["cmo", "coordinator", "cfo", "provider", "it"],
                    "description": "Target persona for demo",
                },
                "duration_minutes": {
                    "type": "integer",
                    "enum": [15, 30, 45],
                    "description": "Demo length",
                },
                "pain_points": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Specific pain points to address (from discovery call)",
                },
            },
            "required": ["persona", "duration_minutes"],
        },
    },
]

# ============================================================================
# PIPELINE AGENT TOOLS (PLANNED - Month 2)
# ============================================================================

PIPELINE_TOOLS = [
    {
        "name": "score_deal",
        "description": "Score a deal (0-100) based on 5 criteria",
        "input_schema": {
            "type": "object",
            "properties": {
                "deal_id": {
                    "type": "string",
                    "description": "CRM deal ID",
                },
                "opportunity_size": {
                    "type": "integer",
                    "description": "ACV in dollars",
                },
                "qualification": {
                    "type": "string",
                    "enum": ["green", "yellow", "red"],
                },
                "decision_timeline": {
                    "type": "string",
                    "enum": ["<30d", "30-60d", "60-90d", ">90d"],
                },
                "buying_signals": {
                    "type": "integer",
                    "minimum": 0,
                    "maximum": 10,
                    "description": "Number of positive buying signals detected",
                },
            },
            "required": ["deal_id", "opportunity_size", "qualification"],
        },
    },
]

# ============================================================================
# COORDINATOR AGENT TOOLS
# ============================================================================

COORDINATOR_TOOLS = [
    {
        "name": "route_task_to_agent",
        "description": "Route incoming customer interaction to appropriate agent",
        "input_schema": {
            "type": "object",
            "properties": {
                "task_type": {
                    "type": "string",
                    "enum": [
                        "new_lead_discovery",
                        "schedule_demo",
                        "handle_objection",
                        "assess_pipeline",
                        "escalate_human",
                    ],
                    "description": "What type of task is this?",
                },
                "agent_name": {
                    "type": "string",
                    "enum": ["discovery", "demo", "objection", "pipeline"],
                    "description": "Which agent should handle this?",
                },
                "customer_context": {
                    "type": "string",
                    "description": "Relevant customer/deal context",
                },
            },
            "required": ["task_type", "agent_name"],
        },
    },
    {
        "name": "get_pipeline_metrics",
        "description": "Get current pipeline health metrics",
        "input_schema": {
            "type": "object",
            "properties": {
                "metric": {
                    "type": "string",
                    "enum": [
                        "total_leads",
                        "stage_distribution",
                        "weighted_pipeline",
                        "forecast",
                        "velocity",
                    ],
                    "description": "Which metric to retrieve",
                },
            },
            "required": ["metric"],
        },
    },
]
