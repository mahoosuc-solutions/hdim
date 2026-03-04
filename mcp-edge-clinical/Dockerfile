FROM node:20-slim
WORKDIR /app

COPY mcp-edge-common/ ./mcp-edge-common/
COPY mcp-edge-clinical/package.json mcp-edge-clinical/package-lock.json* ./mcp-edge-clinical/
WORKDIR /app/mcp-edge-clinical
RUN npm install --omit=dev
COPY mcp-edge-clinical/ ./

ENV NODE_ENV=production
ENV PORT=3300
EXPOSE 3300

HEALTHCHECK --interval=10s --timeout=2s --start-period=5s --retries=6 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||3300)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

USER node
CMD ["node", "index.js"]
