FROM node:20-alpine

RUN apk add --no-cache bash curl docker-cli docker-cli-compose

WORKDIR /app
COPY tools/ops-server/server.js ./server.js

ENV PORT=4710 \
  WORKSPACE_DIR=/workspace \
  COMPOSE_FILE=docker-compose.demo.yml \
  DEMO_SEEDING_URL=http://demo-seeding-service:8098

EXPOSE 4710
CMD ["node", "server.js"]
