#!/usr/bin/env python3
"""
Prompt Reviewer and Grader
Reviews and grades image generation prompts before execution
"""

import json
import re
from pathlib import Path
from typing import Dict, List, Tuple
from datetime import datetime

class PromptReviewer:
    """Review and grade image generation prompts"""
    
    def __init__(self):
        self.criteria = {
            "specificity": {
                "weight": 0.25,
                "description": "How specific and detailed is the prompt?",
                "checks": [
                    ("dimensions", r"(1920|1024|1080|width|height)", "Dimensions specified"),
                    ("colors", r"#[0-9A-Fa-f]{6}|color|rgb|hex", "Color codes specified"),
                    ("layout", r"layout|position|arrangement|structure", "Layout described"),
                    ("style", r"style|aesthetic|design|appearance", "Style specified"),
                    ("elements", r"element|component|widget|button|card|table|chart", "UI elements mentioned"),
                ]
            },
            "completeness": {
                "weight": 0.20,
                "description": "Does the prompt cover all necessary aspects?",
                "checks": [
                    ("layout", r"layout|structure|arrangement", "Layout section present"),
                    ("style", r"style|aesthetic|design", "Style section present"),
                    ("technical", r"technical|resolution|format|quality", "Technical requirements present"),
                    ("avoid", r"avoid|don't|not|exclude", "Negative requirements specified"),
                ]
            },
            "clarity": {
                "weight": 0.20,
                "description": "Is the prompt clear and unambiguous?",
                "checks": [
                    ("structure", r"LAYOUT:|STYLE:|TECHNICAL:|AVOID:", "Clear section headers"),
                    ("specificity", r"\d+|\d+x\d+", "Contains specific numbers/dimensions"),
                    ("examples", r"similar to|like|example|reference", "Contains examples/references"),
                ]
            },
            "brand_consistency": {
                "weight": 0.15,
                "description": "Does the prompt include brand colors and guidelines?",
                "checks": [
                    ("primary_color", r"#1E3A5F|#0066CC|primary.*color|deep.*blue", "Primary brand color specified"),
                    ("accent_color", r"#00A9A5|#00A5B5|accent.*color|teal", "Accent color specified"),
                    ("brand_elements", r"HDIM|logo|brand", "Brand elements mentioned"),
                ]
            },
            "ui_focus": {
                "weight": 0.20,
                "description": "Is the prompt focused on UI/mockup generation?",
                "checks": [
                    ("ui_terms", r"UI|interface|mockup|dashboard|screen|page", "UI-specific terms used"),
                    ("not_photo", r"not.*photo|not.*stock|mockup|wireframe", "Clarifies UI not photo"),
                    ("realistic_ui", r"realistic.*UI|professional.*software|modern.*SaaS", "Emphasizes realistic UI"),
                ]
            }
        }
    
    def grade_prompt(self, prompt: str, prompt_name: str = "Unknown") -> Dict:
        """Grade a single prompt"""
        scores = {}
        total_score = 0
        feedback = []
        
        for criterion_name, criterion in self.criteria.items():
            criterion_score = 0
            max_score = len(criterion["checks"])
            criterion_feedback = []
            
            for check_name, pattern, description in criterion["checks"]:
                if re.search(pattern, prompt, re.IGNORECASE):
                    criterion_score += 1
                    criterion_feedback.append(f"✅ {description}")
                else:
                    criterion_feedback.append(f"❌ Missing: {description}")
            
            criterion_percentage = (criterion_score / max_score) * 100
            weighted_score = criterion_percentage * criterion["weight"]
            
            scores[criterion_name] = {
                "score": criterion_score,
                "max_score": max_score,
                "percentage": criterion_percentage,
                "weighted_score": weighted_score,
                "feedback": criterion_feedback
            }
            
            total_score += weighted_score
            feedback.extend([f"\n{criterion_name.upper().replace('_', ' ')}:"] + criterion_feedback)
        
        # Overall grade
        grade_letter = self._get_grade_letter(total_score)
        
        return {
            "prompt_name": prompt_name,
            "total_score": round(total_score, 2),
            "grade": grade_letter,
            "scores": scores,
            "feedback": feedback,
            "prompt_length": len(prompt),
            "word_count": len(prompt.split())
        }
    
    def _get_grade_letter(self, score: float) -> str:
        """Convert numeric score to letter grade"""
        if score >= 90:
            return "A"
        elif score >= 80:
            return "B"
        elif score >= 70:
            return "C"
        elif score >= 60:
            return "D"
        else:
            return "F"
    
    def review_prompt_file(self, file_path: Path) -> Dict:
        """Review prompts from a Python script file"""
        with open(file_path, 'r') as f:
            content = f.read()
        
        # Extract prompts from the script
        prompts = []
        
        # Look for prompt dictionaries in generate_dashboard_images()
        prompt_pattern = r'"prompt":\s*"""(.*?)"""'
        name_pattern = r'"name":\s*"([^"]+)"'
        
        # Find all prompt blocks
        prompt_blocks = re.finditer(r'\{[^}]*"prompt":\s*"""(.*?)"""[^}]*\}', content, re.DOTALL)
        
        for block in prompt_blocks:
            block_content = block.group(0)
            prompt_match = re.search(r'"prompt":\s*"""(.*?)"""', block_content, re.DOTALL)
            name_match = re.search(r'"name":\s*"([^"]+)"', block_content)
            
            if prompt_match and name_match:
                prompts.append({
                    "name": name_match.group(1),
                    "prompt": prompt_match.group(1).strip()
                })
        
        # If no structured prompts found, try to extract from docstrings or comments
        if not prompts:
            # Look for multi-line strings that look like prompts
            multiline_pattern = r'"""(.*?)"""'
            matches = re.finditer(multiline_pattern, content, re.DOTALL)
            for i, match in enumerate(matches):
                text = match.group(1).strip()
                if len(text) > 200 and ("LAYOUT" in text or "STYLE" in text):
                    prompts.append({
                        "name": f"prompt_{i+1}",
                        "prompt": text
                    })
        
        results = []
        for prompt_data in prompts:
            grade = self.grade_prompt(prompt_data["prompt"], prompt_data["name"])
            results.append(grade)
        
        return {
            "file": str(file_path),
            "prompts_found": len(prompts),
            "results": results,
            "average_score": sum(r["total_score"] for r in results) / len(results) if results else 0
        }
    
    def review_template_file(self, file_path: Path) -> Dict:
        """Review prompts from a YAML template file"""
        import yaml
        
        with open(file_path, 'r') as f:
            template = yaml.safe_load(f)
        
        base_prompt = template.get('template', {}).get('base_prompt', '')
        template_name = template.get('template', {}).get('name', file_path.stem)
        
        grade = self.grade_prompt(base_prompt, template_name)
        
        return {
            "file": str(file_path),
            "template_name": template_name,
            "result": grade
        }
    
    def generate_report(self, reviews: List[Dict], output_path: Path) -> None:
        """Generate a detailed review report"""
        report = []
        report.append("="*80)
        report.append("PROMPT REVIEW REPORT")
        report.append("="*80)
        report.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        overall_scores = []
        
        for review in reviews:
            if "results" in review:  # Multiple prompts in file
                report.append(f"\n{'='*80}")
                report.append(f"FILE: {review['file']}")
                report.append(f"Prompts Found: {review['prompts_found']}")
                report.append(f"Average Score: {review['average_score']:.2f}/100")
                report.append(f"{'='*80}\n")
                
                for result in review["results"]:
                    overall_scores.append(result["total_score"])
                    self._add_result_to_report(report, result)
            
            elif "result" in review:  # Single template
                overall_scores.append(review["result"]["total_score"])
                report.append(f"\n{'='*80}")
                report.append(f"TEMPLATE: {review['template_name']}")
                report.append(f"File: {review['file']}")
                report.append(f"{'='*80}\n")
                self._add_result_to_report(report, review["result"])
        
        # Summary
        if overall_scores:
            avg_score = sum(overall_scores) / len(overall_scores)
            avg_grade = self._get_grade_letter(avg_score)
            
            report.append("\n" + "="*80)
            report.append("OVERALL SUMMARY")
            report.append("="*80)
            report.append(f"Total Prompts Reviewed: {len(overall_scores)}")
            report.append(f"Average Score: {avg_score:.2f}/100")
            report.append(f"Average Grade: {avg_grade}")
            report.append("="*80)
        
        # Write report
        with open(output_path, 'w') as f:
            f.write('\n'.join(report))
        
        print('\n'.join(report))
    
    def _add_result_to_report(self, report: List[str], result: Dict) -> None:
        """Add a single result to the report"""
        report.append(f"\nPrompt: {result['prompt_name']}")
        report.append(f"Score: {result['total_score']:.2f}/100")
        report.append(f"Grade: {result['grade']}")
        report.append(f"Length: {result['prompt_length']} characters, {result['word_count']} words")
        report.append("")
        
        for criterion_name, criterion_data in result['scores'].items():
            report.append(f"  {criterion_name.replace('_', ' ').title()}:")
            report.append(f"    Score: {criterion_data['score']}/{criterion_data['max_score']} ({criterion_data['percentage']:.1f}%)")
            report.append(f"    Weighted: {criterion_data['weighted_score']:.2f} points")
            for feedback_item in criterion_data['feedback'][:3]:  # Show first 3 feedback items
                report.append(f"    {feedback_item}")
            if len(criterion_data['feedback']) > 3:
                report.append(f"    ... and {len(criterion_data['feedback']) - 3} more")
            report.append("")


def main():
    """Main function to review all prompts"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Review and grade image generation prompts")
    parser.add_argument(
        '--script',
        type=Path,
        help='Python script file to review'
    )
    parser.add_argument(
        '--template',
        type=Path,
        help='YAML template file to review'
    )
    parser.add_argument(
        '--directory',
        type=Path,
        help='Directory containing scripts/templates to review'
    )
    parser.add_argument(
        '--output',
        type=Path,
        help='Output report file path'
    )
    
    args = parser.parse_args()
    
    reviewer = PromptReviewer()
    reviews = []
    
    # Review single script
    if args.script:
        print(f"📝 Reviewing script: {args.script}")
        review = reviewer.review_prompt_file(args.script)
        reviews.append(review)
    
    # Review single template
    if args.template:
        print(f"📝 Reviewing template: {args.template}")
        review = reviewer.review_template_file(args.template)
        reviews.append(review)
    
    # Review directory
    if args.directory:
        print(f"📝 Reviewing directory: {args.directory}")
        script_dir = args.directory / "scripts"
        template_dir = args.directory / "templates"
        
        if script_dir.exists():
            for script_file in script_dir.glob("*.py"):
                if "generate" in script_file.name.lower():
                    print(f"  Reviewing: {script_file.name}")
                    review = reviewer.review_prompt_file(script_file)
                    reviews.append(review)
        
        if template_dir.exists():
            for template_file in template_dir.glob("*.yaml"):
                print(f"  Reviewing: {template_file.name}")
                review = reviewer.review_template_file(template_file)
                reviews.append(review)
    
    # Default: Review AG-UI directory
    if not args.script and not args.template and not args.directory:
        ag_ui_dir = Path(__file__).parent.parent
        print(f"📝 Reviewing AG-UI directory: {ag_ui_dir}")
        
        # Review scripts
        script_dir = ag_ui_dir / "scripts"
        if script_dir.exists():
            for script_file in script_dir.glob("generate*.py"):
                print(f"  Reviewing: {script_file.name}")
                review = reviewer.review_prompt_file(script_file)
                reviews.append(review)
        
        # Review templates
        template_dir = ag_ui_dir / "templates"
        if template_dir.exists():
            for template_file in template_dir.glob("*.yaml"):
                print(f"  Reviewing: {template_file.name}")
                review = reviewer.review_template_file(template_file)
                reviews.append(review)
    
    # Generate report
    if reviews:
        output_path = args.output or (Path(__file__).parent.parent / "metadata" / f"prompt-review-{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        reviewer.generate_report(reviews, output_path)
        print(f"\n✅ Report saved to: {output_path}")
    else:
        print("❌ No prompts found to review")


if __name__ == "__main__":
    main()
