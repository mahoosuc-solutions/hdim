# HealthData-in-Motion - Docker Management Makefile
# Convenience commands for managing the Docker stack

.PHONY: help build up down restart logs ps clean test health status

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(BLUE)HealthData-in-Motion - Docker Management$(NC)"
	@echo ""
	@echo "$(GREEN)Available commands:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'

# ============================================================================
# Setup and Configuration
# ============================================================================

setup: ## Initial setup - copy .env and create directories
	@echo "$(BLUE)Setting up environment...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN)✓ Created .env file from .env.example$(NC)"; \
	else \
		echo "$(YELLOW)⚠ .env already exists, skipping$(NC)"; \
	fi
	@mkdir -p logs/cql-engine
	@mkdir -p docker/grafana/dashboards
	@mkdir -p docker/grafana/datasources
	@echo "$(GREEN)✓ Created log directories$(NC)"
	@echo "$(GREEN)✓ Setup complete!$(NC)"

# ============================================================================
# Docker Compose Commands
# ============================================================================

build: ## Build all Docker images
	@echo "$(BLUE)Building Docker images...$(NC)"
	docker compose build --no-cache
	@echo "$(GREEN)✓ Build complete$(NC)"

up: ## Start all services (full profile - all 22 services)
	@echo "$(BLUE)Starting all services...$(NC)"
	docker compose --profile full up -d
	@echo "$(GREEN)✓ All 22 services started$(NC)"
	@echo "$(YELLOW)Run 'make logs' to view logs$(NC)"
	@echo "$(YELLOW)Run 'make health' to check service health$(NC)"

up-core: ## Start core clinical services only (10 services)
	@echo "$(BLUE)Starting core clinical services...$(NC)"
	docker compose --profile core up -d
	@echo "$(GREEN)✓ Core services started (Gateway, FHIR, CQL, Patient, Care-Gap, Quality-Measure, Event-Processing, Event-Router, Consent, Clinical-Portal)$(NC)"

up-ai: ## Start AI services only (Agent Runtime, AI Assistant, Agent Builder)
	@echo "$(BLUE)Starting AI services...$(NC)"
	docker compose --profile ai up -d
	@echo "$(GREEN)✓ AI services started (Agent Runtime, AI Assistant, Agent Builder)$(NC)"

up-analytics: ## Start analytics services only (Analytics, Predictive, SDOH)
	@echo "$(BLUE)Starting analytics services...$(NC)"
	docker compose --profile analytics up -d
	@echo "$(GREEN)✓ Analytics services started (Analytics, Predictive Analytics, SDOH)$(NC)"

up-monitoring: ## Start all services including monitoring (Prometheus + Grafana)
	@echo "$(BLUE)Starting all services with monitoring...$(NC)"
	docker compose --profile monitoring up -d
	@echo "$(GREEN)✓ Services started with monitoring$(NC)"

down: ## Stop all services
	@echo "$(BLUE)Stopping all services...$(NC)"
	docker compose down
	@echo "$(GREEN)✓ Services stopped$(NC)"

down-volumes: ## Stop all services and remove volumes (WARNING: Deletes data!)
	@echo "$(RED)WARNING: This will delete all data!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose down -v; \
		echo "$(GREEN)✓ Services stopped and volumes removed$(NC)"; \
	else \
		echo "$(YELLOW)Aborted$(NC)"; \
	fi

restart: ## Restart all services
	@echo "$(BLUE)Restarting all services...$(NC)"
	docker compose restart
	@echo "$(GREEN)✓ Services restarted$(NC)"

restart-cql: ## Restart only CQL Engine Service
	@echo "$(BLUE)Restarting CQL Engine Service...$(NC)"
	docker compose restart cql-engine-service
	@echo "$(GREEN)✓ CQL Engine Service restarted$(NC)"

# ============================================================================
# Monitoring and Logs
# ============================================================================

logs: ## View logs from all services
	docker compose logs -f

logs-cql: ## View logs from CQL Engine Service only
	docker compose logs -f cql-engine-service

logs-db: ## View logs from PostgreSQL
	docker compose logs -f postgres

logs-redis: ## View logs from Redis
	docker compose logs -f redis

logs-kafka: ## View logs from Kafka
	docker compose logs -f kafka

ps: ## List running containers
	@docker compose ps

status: ## Show service status with health checks
	@echo "$(BLUE)Service Status:$(NC)"
	@docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

health: ## Check health of all services
	@echo "$(BLUE)Checking service health...$(NC)"
	@echo ""
	@echo "$(YELLOW)Infrastructure:$(NC)"
	@docker compose exec -T postgres pg_isready -U healthdata && echo "$(GREEN)✓ PostgreSQL ready$(NC)" || echo "$(RED)✗ PostgreSQL not ready$(NC)"
	@docker compose exec -T redis redis-cli ping > /dev/null 2>&1 && echo "$(GREEN)✓ Redis ready$(NC)" || echo "$(RED)✗ Redis not responding$(NC)"
	@docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1 && echo "$(GREEN)✓ Kafka ready$(NC)" || echo "$(RED)✗ Kafka not ready$(NC)"
	@echo ""
	@echo "$(YELLOW)Core Services:$(NC)"
	@curl -sf http://localhost:8080/actuator/health > /dev/null && echo "$(GREEN)✓ Gateway (8080)$(NC)" || echo "$(RED)✗ Gateway (8080)$(NC)"
	@curl -sf http://localhost:8081/actuator/health > /dev/null && echo "$(GREEN)✓ CQL Engine (8081)$(NC)" || echo "$(RED)✗ CQL Engine (8081)$(NC)"
	@curl -sf http://localhost:8082/actuator/health > /dev/null && echo "$(GREEN)✓ Consent (8082)$(NC)" || echo "$(RED)✗ Consent (8082)$(NC)"
	@curl -sf http://localhost:8083/actuator/health > /dev/null && echo "$(GREEN)✓ Event Processing (8083)$(NC)" || echo "$(RED)✗ Event Processing (8083)$(NC)"
	@curl -sf http://localhost:8084/actuator/health > /dev/null && echo "$(GREEN)✓ Patient (8084)$(NC)" || echo "$(RED)✗ Patient (8084)$(NC)"
	@curl -sf http://localhost:8085/actuator/health > /dev/null && echo "$(GREEN)✓ FHIR (8085)$(NC)" || echo "$(RED)✗ FHIR (8085)$(NC)"
	@curl -sf http://localhost:8086/actuator/health > /dev/null && echo "$(GREEN)✓ Care Gap (8086)$(NC)" || echo "$(RED)✗ Care Gap (8086)$(NC)"
	@curl -sf http://localhost:8087/actuator/health > /dev/null && echo "$(GREEN)✓ Quality Measure (8087)$(NC)" || echo "$(RED)✗ Quality Measure (8087)$(NC)"
	@echo ""
	@echo "$(YELLOW)AI Services:$(NC)"
	@curl -sf http://localhost:8088/actuator/health > /dev/null && echo "$(GREEN)✓ Agent Runtime (8088)$(NC)" || echo "$(RED)✗ Agent Runtime (8088)$(NC)"
	@curl -sf http://localhost:8090/actuator/health > /dev/null && echo "$(GREEN)✓ AI Assistant (8090)$(NC)" || echo "$(RED)✗ AI Assistant (8090)$(NC)"
	@curl -sf http://localhost:8096/actuator/health > /dev/null && echo "$(GREEN)✓ Agent Builder (8096)$(NC)" || echo "$(RED)✗ Agent Builder (8096)$(NC)"
	@echo ""
	@echo "$(YELLOW)Analytics Services:$(NC)"
	@curl -sf http://localhost:8092/actuator/health > /dev/null && echo "$(GREEN)✓ Analytics (8092)$(NC)" || echo "$(RED)✗ Analytics (8092)$(NC)"
	@curl -sf http://localhost:8093/actuator/health > /dev/null && echo "$(GREEN)✓ Predictive Analytics (8093)$(NC)" || echo "$(RED)✗ Predictive Analytics (8093)$(NC)"
	@curl -sf http://localhost:8094/actuator/health > /dev/null && echo "$(GREEN)✓ SDOH (8094)$(NC)" || echo "$(RED)✗ SDOH (8094)$(NC)"
	@echo ""
	@echo "$(YELLOW)Full Profile Services:$(NC)"
	@curl -sf http://localhost:8089/actuator/health > /dev/null && echo "$(GREEN)✓ Data Enrichment (8089)$(NC)" || echo "$(RED)✗ Data Enrichment (8089)$(NC)"
	@curl -sf http://localhost:8091/actuator/health > /dev/null && echo "$(GREEN)✓ Documentation (8091)$(NC)" || echo "$(RED)✗ Documentation (8091)$(NC)"
	@curl -sf http://localhost:8095/actuator/health > /dev/null && echo "$(GREEN)✓ Event Router (8095)$(NC)" || echo "$(RED)✗ Event Router (8095)$(NC)"
	@curl -sf http://localhost:8097/actuator/health > /dev/null && echo "$(GREEN)✓ Approval (8097)$(NC)" || echo "$(RED)✗ Approval (8097)$(NC)"
	@curl -sf http://localhost:8098/actuator/health > /dev/null && echo "$(GREEN)✓ Payer Workflows (8098)$(NC)" || echo "$(RED)✗ Payer Workflows (8098)$(NC)"
	@curl -sf http://localhost:8099/actuator/health > /dev/null && echo "$(GREEN)✓ CDR Processor (8099)$(NC)" || echo "$(RED)✗ CDR Processor (8099)$(NC)"
	@curl -sf http://localhost:8100/actuator/health > /dev/null && echo "$(GREEN)✓ EHR Connector (8100)$(NC)" || echo "$(RED)✗ EHR Connector (8100)$(NC)"

# ============================================================================
# Database Operations
# ============================================================================

db-connect: ## Connect to PostgreSQL database
	docker compose exec postgres psql -U healthdata -d healthdata_cql

db-backup: ## Backup PostgreSQL database
	@echo "$(BLUE)Creating database backup...$(NC)"
	@mkdir -p backups
	@docker compose exec -T postgres pg_dump -U healthdata healthdata_cql > backups/healthdata_cql_$$(date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✓ Backup created in backups/ directory$(NC)"

db-restore: ## Restore PostgreSQL database from backup (Usage: make db-restore FILE=backup.sql)
	@if [ -z "$(FILE)" ]; then \
		echo "$(RED)Error: Please specify FILE=backup.sql$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Restoring database from $(FILE)...$(NC)"
	@docker compose exec -T postgres psql -U healthdata -d healthdata_cql < $(FILE)
	@echo "$(GREEN)✓ Database restored$(NC)"

db-reset: ## Reset database (WARNING: Deletes all data!)
	@echo "$(RED)WARNING: This will delete all database data!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker compose exec postgres psql -U healthdata -c "DROP DATABASE IF EXISTS healthdata_cql;"; \
		docker compose exec postgres psql -U healthdata -c "CREATE DATABASE healthdata_cql;"; \
		docker compose restart cql-engine-service; \
		echo "$(GREEN)✓ Database reset$(NC)"; \
	else \
		echo "$(YELLOW)Aborted$(NC)"; \
	fi

# ============================================================================
# Testing and Development
# ============================================================================

test-api: ## Test CQL Engine API endpoints
	@echo "$(BLUE)Testing CQL Engine API...$(NC)"
	@echo ""
	@echo "$(YELLOW)1. Health Check:$(NC)"
	@curl -s http://localhost:8081/actuator/health | jq '.'
	@echo ""
	@echo "$(YELLOW)2. List All Measures:$(NC)"
	@curl -s http://localhost:8081/api/v1/measures | jq '. | length' | xargs echo "Measures found:"
	@echo ""
	@echo "$(YELLOW)3. Swagger UI:$(NC)"
	@echo "http://localhost:8081/swagger-ui.html"

test-fhir: ## Test FHIR Server
	@echo "$(BLUE)Testing FHIR Server...$(NC)"
	@curl -s http://localhost:8080/fhir/metadata | jq '.fhirVersion' || echo "$(RED)✗ FHIR Server not responding$(NC)"

shell-cql: ## Open shell in CQL Engine Service container
	docker compose exec cql-engine-service sh

shell-db: ## Open shell in PostgreSQL container
	docker compose exec postgres bash

shell-redis: ## Open Redis CLI
	docker compose exec redis redis-cli

# ============================================================================
# Cleanup
# ============================================================================

clean: ## Clean up containers, images, and volumes
	@echo "$(BLUE)Cleaning up...$(NC)"
	docker compose down -v --rmi local
	@echo "$(GREEN)✓ Cleanup complete$(NC)"

clean-logs: ## Remove log files
	@echo "$(BLUE)Cleaning log files...$(NC)"
	rm -rf logs/*
	@echo "$(GREEN)✓ Logs cleaned$(NC)"

prune: ## Remove all unused Docker resources (WARNING: Aggressive cleanup!)
	@echo "$(RED)WARNING: This will remove all unused Docker resources!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker system prune -a --volumes; \
		echo "$(GREEN)✓ Docker system pruned$(NC)"; \
	else \
		echo "$(YELLOW)Aborted$(NC)"; \
	fi

# ============================================================================
# Monitoring (when monitoring profile is active)
# ============================================================================

prometheus: ## Open Prometheus in browser
	@echo "$(BLUE)Opening Prometheus...$(NC)"
	@open http://localhost:9090 || xdg-open http://localhost:9090 || echo "$(YELLOW)Open http://localhost:9090 in your browser$(NC)"

grafana: ## Open Grafana in browser
	@echo "$(BLUE)Opening Grafana...$(NC)"
	@echo "$(YELLOW)Default credentials: admin / admin$(NC)"
	@open http://localhost:3000 || xdg-open http://localhost:3000 || echo "$(YELLOW)Open http://localhost:3000 in your browser$(NC)"

# ============================================================================
# Build and Deploy
# ============================================================================

rebuild: down build up ## Rebuild images and restart services

rebuild-cql: ## Rebuild only CQL Engine Service
	@echo "$(BLUE)Rebuilding CQL Engine Service...$(NC)"
	docker compose build cql-engine-service
	docker compose up -d cql-engine-service
	@echo "$(GREEN)✓ CQL Engine Service rebuilt and restarted$(NC)"

deploy: ## Full deployment sequence (setup, build, up)
	@echo "$(BLUE)Deploying HealthData-in-Motion...$(NC)"
	@make setup
	@make build
	@make up
	@echo ""
	@echo "$(GREEN)✓ Deployment complete!$(NC)"
	@echo ""
	@echo "$(YELLOW)Services:$(NC)"
	@echo "  CQL Engine API: http://localhost:8081"
	@echo "  Swagger UI: http://localhost:8081/swagger-ui.html"
	@echo "  FHIR Server: http://localhost:8080/fhir"
	@echo ""
	@echo "$(YELLOW)Check status: make health$(NC)"

# ============================================================================
# Information
# ============================================================================

info: ## Show system information
	@echo "$(BLUE)System Information:$(NC)"
	@echo ""
	@echo "$(YELLOW)Docker Version:$(NC)"
	@docker --version
	@echo ""
	@echo "$(YELLOW)Docker Compose Version:$(NC)"
	@docker compose --version
	@echo ""
	@echo "$(YELLOW)Running Containers:$(NC)"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep healthdata || echo "No containers running"
	@echo ""
	@echo "$(YELLOW)Disk Usage:$(NC)"
	@docker system df

version: ## Show service versions
	@echo "$(BLUE)Service Versions:$(NC)"
	@echo ""
	@echo "$(YELLOW)CQL Engine Service:$(NC) 1.0.0"
	@echo "$(YELLOW)PostgreSQL:$(NC) 16-alpine"
	@echo "$(YELLOW)Redis:$(NC) 7-alpine"
	@echo "$(YELLOW)Kafka:$(NC) 7.5.0"
	@echo "$(YELLOW)HAPI FHIR:$(NC) latest"
