# Build stage
FROM node:20-alpine AS builder

WORKDIR /workspace

# Copy root workspace files
COPY nx.json package.json package-lock.json tsconfig.base.json ./

# Copy coaching-ui app
COPY apps/coaching-ui ./apps/coaching-ui

# Install dependencies
RUN npm install --legacy-peer-deps --omit=optional

# Build Angular app using Nx
RUN npx nx build coaching-ui --configuration production

# Production stage
FROM nginx:alpine

# Copy nginx configuration
COPY apps/coaching-ui/nginx.conf /etc/nginx/nginx.conf

# Copy built application from builder stage
COPY --from=builder /workspace/dist/apps/coaching-ui /usr/share/nginx/html

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:4200/ || exit 1

# Expose port
EXPOSE 4200

# Run nginx
CMD ["nginx", "-g", "daemon off;"]
