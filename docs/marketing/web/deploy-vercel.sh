#!/bin/bash
# Deploy AI Solutioning Journey web content to Vercel

set -e

echo "🚀 Deploying to Vercel..."
echo ""

# Check if Vercel CLI is installed
if ! command -v vercel &> /dev/null; then
    echo "❌ Vercel CLI not found. Installing..."
    npm install -g vercel
    echo "✅ Vercel CLI installed"
    echo ""
fi

# Navigate to web directory
cd "$(dirname "$0")"

echo "📁 Current directory: $(pwd)"
echo "📄 Files to deploy:"
ls -1 *.html | head -5
echo "..."

# Check if already logged in
if ! vercel whoami &> /dev/null; then
    echo "🔐 Please log in to Vercel..."
    vercel login
fi

echo ""
echo "🌐 Deploying to Vercel..."
echo ""

# Deploy to production
vercel --prod --yes

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📝 Next steps:"
echo "1. Update BASE_URL in convert-to-html-enhanced.py with your Vercel domain"
echo "2. Update sitemap.xml with your Vercel domain"
echo "3. Regenerate HTML files if needed"
echo "4. Configure custom domain in Vercel dashboard (optional)"
echo ""
