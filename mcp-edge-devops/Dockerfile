FROM node:20-slim
WORKDIR /app

# Docker CLI and healthcheck probes require ca-certificates and curl
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl && rm -rf /var/lib/apt/lists/*

COPY mcp-edge-common/ ./mcp-edge-common/
COPY mcp-edge-devops/package.json mcp-edge-devops/package-lock.json* ./mcp-edge-devops/
WORKDIR /app/mcp-edge-devops
RUN npm install --omit=dev
COPY mcp-edge-devops/ ./

ENV NODE_ENV=production
ENV PORT=3200
EXPOSE 3200

HEALTHCHECK --interval=10s --timeout=2s --start-period=5s --retries=6 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||3200)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

USER node
CMD ["node", "index.js"]
