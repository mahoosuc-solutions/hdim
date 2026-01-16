#!/usr/bin/env python3
"""
Generate demo narration JSON using the AG-UI generator infrastructure.
Reads storyboard steps and outputs narration text per step.
"""

import json
import os
import sys
from pathlib import Path
import importlib.util

ROOT_DIR = Path(__file__).resolve().parents[3]
STORYBOARD_PATH = ROOT_DIR / "apps" / "clinical-portal" / "public" / "demo" / "storyboard.json"
OUTPUT_PATH = ROOT_DIR / "apps" / "clinical-portal" / "public" / "demo" / "narration.json"
AG_UI_GENERATOR_PATH = Path(__file__).resolve().parent / "ag-ui-generator.py"


def load_ag_ui_generator():
  spec = importlib.util.spec_from_file_location("ag_ui_generator", AG_UI_GENERATOR_PATH)
  module = importlib.util.module_from_spec(spec)
  if spec and spec.loader:
    spec.loader.exec_module(module)
    return module
  raise RuntimeError("Unable to load ag-ui-generator.py")


def main():
  if not STORYBOARD_PATH.exists():
    raise FileNotFoundError(f"Storyboard not found: {STORYBOARD_PATH}")

  with STORYBOARD_PATH.open("r", encoding="utf-8") as handle:
    storyboard = json.load(handle)

  steps = storyboard.get("steps", [])
  if not steps:
    raise ValueError("Storyboard has no steps")

  ag_ui = load_ag_ui_generator()
  generator = ag_ui.AGUIGenerator(ai_tool="dalle-3")
  client = generator.client

  model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
  narration = {"version": "1.0", "steps": {}}

  for step in steps:
    step_id = step.get("id")
    title = step.get("title", "Demo Step")
    prompt = (
      "Write a concise, customer-facing narration line for a healthcare analytics demo. "
      "Focus on outcomes, clarity, and confidence. "
      f"Step title: {title}. "
      f"Context: {step.get('narration', '')}"
    )
    response = client.chat.completions.create(
      model=model,
      messages=[
        {"role": "system", "content": "You are a clinical demo narrator."},
        {"role": "user", "content": prompt},
      ],
      temperature=0.4,
    )
    text = response.choices[0].message.content.strip()
    narration["steps"][step_id] = {"text": text}

  OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
  with OUTPUT_PATH.open("w", encoding="utf-8") as handle:
    json.dump(narration, handle, indent=2)

  print(f"Generated narration at {OUTPUT_PATH}")


if __name__ == "__main__":
  try:
    main()
  except Exception as exc:
    print(f"Error: {exc}")
    sys.exit(1)
