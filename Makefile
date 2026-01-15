.PHONY: build clean compile test run package help dev-start dev-stop dev-clean db-logs db-psql ollama-pull ollama-logs run-local

help: ## Zeigt diese Hilfe an
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: clean compile test ## Vollständiger Build (clean, compile, test)

clean: ## Bereinigt das Projekt
	./mvnw clean

compile: ## Kompiliert das Projekt
	./mvnw compile

test: ## Führt alle Tests aus
	./mvnw test

run: ## Startet die Anwendung
	./mvnw spring-boot:run

package: ## Erstellt das JAR-Archiv
	./mvnw package -DskipTests

verify: ## Führt alle Prüfungen inkl. Integration-Tests aus
	./mvnw verify

# Development Environment (Docker Compose: PostgreSQL + Ollama)
dev-start: ## Startet alle Dev-Container (PostgreSQL + Ollama)
	docker compose -f misc/docker-compose.yml up -d

dev-stop: ## Stoppt alle Dev-Container
	docker compose -f misc/docker-compose.yml down

dev-clean: ## Stoppt Dev-Container und löscht Volumes
	docker compose -f misc/docker-compose.yml down -v

# Database Commands
db-logs: ## Zeigt PostgreSQL Logs an
	docker compose -f misc/docker-compose.yml logs -f postgres

db-psql: ## Öffnet PostgreSQL CLI
	docker exec -it pf2e-postgres psql -U pf2e -d pf2e_oracle

# Ollama Commands
ollama-pull: ## Lädt die benötigten Ollama Modelle
	docker exec -it pf2e-ollama ollama pull nomic-embed-text
	docker exec -it pf2e-ollama ollama pull llama3.2

ollama-logs: ## Zeigt Ollama Logs an
	docker compose -f misc/docker-compose.yml logs -f ollama

# Run with Profiles
run-local: dev-start ## Startet Anwendung mit lokaler DB und Ollama (docker-compose)
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local
