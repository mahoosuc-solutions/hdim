#!/usr/bin/env python3
"""
AG-UI Generator: AI-Generated UI Content System
Generates UI mockups, screenshots, and visual assets using AI tools
"""

import os
import json
import yaml
import argparse
from pathlib import Path
from typing import Dict, List, Optional
from datetime import datetime
import openai
from openai import OpenAI
import requests
from PIL import Image
import io

# Configuration
CONFIG_DIR = Path(__file__).parent.parent
TEMPLATES_DIR = CONFIG_DIR / "templates"
ASSETS_DIR = CONFIG_DIR / "assets"
METADATA_DIR = CONFIG_DIR / "metadata"

# Ensure directories exist
ASSETS_DIR.mkdir(parents=True, exist_ok=True)
METADATA_DIR.mkdir(parents=True, exist_ok=True)


class AGUIGenerator:
    """Main class for generating AI UI content"""
    
    def __init__(self, ai_tool: str = "dalle-3", api_key: Optional[str] = None):
        self.ai_tool = ai_tool
        self.api_key = api_key or os.getenv("OPENAI_API_KEY")
        
        if ai_tool == "dalle-3":
            if not self.api_key:
                raise ValueError("OPENAI_API_KEY environment variable required")
            self.client = OpenAI(api_key=self.api_key)
        elif ai_tool == "stable-diffusion":
            self.stability_api_key = os.getenv("STABILITY_API_KEY")
            if not self.stability_api_key:
                raise ValueError("STABILITY_API_KEY environment variable required")
    
    def load_template(self, template_name: str) -> Dict:
        """Load a prompt template from YAML file"""
        template_path = TEMPLATES_DIR / f"{template_name}.yaml"
        
        if not template_path.exists():
            raise FileNotFoundError(f"Template not found: {template_path}")
        
        with open(template_path, 'r') as f:
            template = yaml.safe_load(f)
        
        return template
    
    def render_prompt(self, template: Dict, parameters: Dict) -> str:
        """Render a prompt template with parameters"""
        base_prompt = template.get('base_prompt', '')
        
        # Replace parameters in prompt
        for key, value in parameters.items():
            base_prompt = base_prompt.replace(f"{{{key}}}", str(value))
        
        return base_prompt
    
    def generate_with_dalle3(
        self,
        prompt: str,
        size: str = "1024x1024",
        quality: str = "hd",
        style: str = "vivid"
    ) -> str:
        """Generate image using OpenAI DALL-E 3"""
        try:
            response = self.client.images.generate(
                model="dall-e-3",
                prompt=prompt,
                size=size,
                quality=quality,
                style=style,
                n=1
            )
            
            image_url = response.data[0].url
            return image_url
        
        except Exception as e:
            raise Exception(f"DALL-E 3 generation failed: {str(e)}")
    
    def generate_with_stable_diffusion(
        self,
        prompt: str,
        negative_prompt: str = "",
        width: int = 1024,
        height: int = 1024,
        steps: int = 30,
        cfg_scale: float = 7.0
    ) -> bytes:
        """Generate image using Stability AI Stable Diffusion"""
        try:
            response = requests.post(
                "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image",
                headers={
                    "Authorization": f"Bearer {self.stability_api_key}",
                    "Content-Type": "application/json"
                },
                json={
                    "text_prompts": [
                        {"text": prompt, "weight": 1.0},
                        {"text": negative_prompt, "weight": -1.0} if negative_prompt else None
                    ],
                    "cfg_scale": cfg_scale,
                    "steps": steps,
                    "width": width,
                    "height": height,
                    "samples": 1
                }
            )
            
            if response.status_code != 200:
                raise Exception(f"Stable Diffusion API error: {response.text}")
            
            result = response.json()
            image_base64 = result["artifacts"][0]["base64"]
            image_bytes = io.BytesIO(image_base64.decode('base64'))
            
            return image_bytes.getvalue()
        
        except Exception as e:
            raise Exception(f"Stable Diffusion generation failed: {str(e)}")
    
    def download_image(self, url: str, output_path: Path) -> None:
        """Download image from URL"""
        response = requests.get(url)
        response.raise_for_status()
        
        with open(output_path, 'wb') as f:
            f.write(response.content)
    
    def save_image(self, image_data: bytes, output_path: Path) -> None:
        """Save image data to file"""
        with open(output_path, 'wb') as f:
            f.write(image_data)
    
    def optimize_image(self, image_path: Path, max_size: int = 5 * 1024 * 1024) -> None:
        """Optimize image file size"""
        with Image.open(image_path) as img:
            # Convert to RGB if necessary
            if img.mode != 'RGB':
                img = img.convert('RGB')
            
            # Optimize quality if file is too large
            if image_path.stat().st_size > max_size:
                quality = 85
                while image_path.stat().st_size > max_size and quality > 50:
                    img.save(image_path, 'PNG', optimize=True, quality=quality)
                    quality -= 5
    
    def generate_ui(
        self,
        template_name: str,
        variation: Optional[str] = None,
        parameters: Optional[Dict] = None,
        output_path: Optional[Path] = None
    ) -> Dict:
        """Generate UI mockup from template"""
        # Load template
        template = self.load_template(template_name)
        
        # Get variation or use default
        if variation:
            variations = template.get('variations', [])
            variation_config = next(
                (v for v in variations if v['name'] == variation),
                None
            )
            if variation_config:
                template_params = {**template.get('parameters', {}), **variation_config.get('parameters', {})}
            else:
                template_params = template.get('parameters', {})
        else:
            template_params = template.get('parameters', {})
        
        # Merge with provided parameters
        final_params = {**template_params, **(parameters or {})}
        
        # Render prompt
        prompt = self.render_prompt(template, final_params)
        
        # Generate image
        print(f"🎨 Generating UI mockup: {template_name} ({variation or 'default'})")
        print(f"📝 Prompt: {prompt[:100]}...")
        
        if self.ai_tool == "dalle-3":
            size = final_params.get('size', '1024x1024')
            quality = final_params.get('quality', 'hd')
            image_url = self.generate_with_dalle3(prompt, size=size, quality=quality)
            
            # Determine output path
            if not output_path:
                category = template.get('category', 'general')
                filename = f"{template_name}-{variation or 'default'}.png"
                output_path = ASSETS_DIR / category / filename
                output_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Download image
            self.download_image(image_url, output_path)
            print(f"✅ Generated: {output_path}")
        
        elif self.ai_tool == "stable-diffusion":
            width = final_params.get('width', 1024)
            height = final_params.get('height', 1024)
            negative_prompt = template.get('negative_prompt', '')
            image_data = self.generate_with_stable_diffusion(
                prompt,
                negative_prompt=negative_prompt,
                width=width,
                height=height
            )
            
            # Determine output path
            if not output_path:
                category = template.get('category', 'general')
                filename = f"{template_name}-{variation or 'default'}.png"
                output_path = ASSETS_DIR / category / filename
                output_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Save image
            self.save_image(image_data, output_path)
            print(f"✅ Generated: {output_path}")
        
        # Optimize image
        self.optimize_image(output_path)
        
        # Save metadata
        metadata = {
            "asset_id": f"{template_name}-{variation or 'default'}-{datetime.now().strftime('%Y%m%d%H%M%S')}",
            "name": f"{template_name} - {variation or 'default'}",
            "category": template.get('category', 'general'),
            "template": template_name,
            "variation": variation,
            "ai_tool": self.ai_tool,
            "prompt": prompt,
            "parameters": final_params,
            "generated_at": datetime.now().isoformat(),
            "file_path": str(output_path.relative_to(CONFIG_DIR)),
            "file_size": output_path.stat().st_size,
            "dimensions": {
                "width": final_params.get('width', 1024),
                "height": final_params.get('height', 1024)
            }
        }
        
        metadata_path = METADATA_DIR / f"{metadata['asset_id']}.json"
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        return metadata
    
    def batch_generate(self, config_path: Path) -> List[Dict]:
        """Generate multiple UI mockups from config file"""
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
        
        results = []
        
        for item in config.get('items', []):
            template_name = item['template']
            variations = item.get('variations', [None])
            
            for variation in variations:
                try:
                    metadata = self.generate_ui(
                        template_name=template_name,
                        variation=variation,
                        parameters=item.get('parameters')
                    )
                    results.append(metadata)
                except Exception as e:
                    print(f"❌ Error generating {template_name} ({variation}): {str(e)}")
                    results.append({
                        "template": template_name,
                        "variation": variation,
                        "error": str(e)
                    })
        
        return results


def main():
    parser = argparse.ArgumentParser(description="AG-UI Generator: AI-Generated UI Content System")
    parser.add_argument(
        'command',
        choices=['generate', 'batch-generate'],
        help='Command to execute'
    )
    parser.add_argument(
        '--template',
        help='Template name for single generation'
    )
    parser.add_argument(
        '--variation',
        help='Template variation name'
    )
    parser.add_argument(
        '--config',
        type=Path,
        help='Config file path for batch generation'
    )
    parser.add_argument(
        '--output',
        type=Path,
        help='Output file path'
    )
    parser.add_argument(
        '--ai-tool',
        choices=['dalle-3', 'stable-diffusion'],
        default='dalle-3',
        help='AI tool to use'
    )
    parser.add_argument(
        '--parameters',
        help='JSON string of parameters to override'
    )
    
    args = parser.parse_args()
    
    # Initialize generator
    generator = AGUIGenerator(ai_tool=args.ai_tool)
    
    # Parse parameters if provided
    parameters = None
    if args.parameters:
        parameters = json.loads(args.parameters)
    
    # Execute command
    if args.command == 'generate':
        if not args.template:
            parser.error("--template required for generate command")
        
        metadata = generator.generate_ui(
            template_name=args.template,
            variation=args.variation,
            parameters=parameters,
            output_path=args.output
        )
        
        print(f"\n✅ Generation complete!")
        print(f"📁 Asset ID: {metadata['asset_id']}")
        print(f"📂 File: {metadata['file_path']}")
    
    elif args.command == 'batch-generate':
        if not args.config:
            parser.error("--config required for batch-generate command")
        
        results = generator.batch_generate(args.config)
        
        print(f"\n✅ Batch generation complete!")
        print(f"📊 Generated: {len([r for r in results if 'error' not in r])} successful")
        print(f"❌ Failed: {len([r for r in results if 'error' in r])} failed")


if __name__ == "__main__":
    main()
