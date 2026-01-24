FROM node:22-bookworm-slim

# Install Chromium and dependencies for headless rendering
RUN apt-get update && apt-get install -y \
    chromium \
    libnss3 \
    libxss1 \
    libasound2 \
    fonts-noto-color-emoji \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables for Chrome
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMIUM_PATH=/usr/bin/chromium
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Create output directory and resolve symlinks
RUN mkdir -p out && \
    if [ -L public/screenshots ]; then \
      rm -f public/screenshots && \
      mkdir -p public/screenshots; \
    fi

# Default command: render 90s version
CMD ["npm", "run", "render:90s"]
