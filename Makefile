.PHONY: build clean compile test run package help dev-start dev-stop dev-clean db-logs db-psql ollama-pull ollama-logs run-local

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: clean compile test ## Full build (clean, compile, test)

clean: ## Clean the project
	./mvnw clean

compile: ## Compile the project
	./mvnw compile

test: ## Run all tests
	./mvnw test

run: ## Start the application
	./mvnw spring-boot:run

package: ## Create the JAR archive (includes tests)
	./mvnw package

verify: ## Run all checks including integration tests
	./mvnw verify

# Development Environment (Docker Compose: PostgreSQL + Ollama)
dev-start: ## Start all dev containers (PostgreSQL + Ollama)
	docker compose -f misc/docker-compose.yml up -d

dev-stop: ## Stop all dev containers
	docker compose -f misc/docker-compose.yml down

dev-clean: ## Stop dev containers and delete volumes
	docker compose -f misc/docker-compose.yml down -v

# Database Commands
db-logs: ## Show PostgreSQL logs
	docker compose -f misc/docker-compose.yml logs -f postgres

db-psql: ## Open PostgreSQL CLI
	docker exec -it pf2e-postgres psql -U pf2e -d pf2e_oracle

# Ollama Commands
ollama-pull: ## Pull required Ollama models
	docker exec -it pf2e-ollama ollama pull nomic-embed-text
	docker exec -it pf2e-ollama ollama pull llama3.2

ollama-logs: ## Show Ollama logs
	docker compose -f misc/docker-compose.yml logs -f ollama

# Run with Profiles
run-local: dev-start ## Start application with local DB and Ollama (docker-compose)
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local
