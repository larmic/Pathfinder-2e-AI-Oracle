.PHONY: build clean compile test run package help db-start db-stop db-clean db-logs db-psql run-local

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

# Database Commands
db-start: ## Startet PostgreSQL Container
	docker compose -f misc/docker-compose.yml up -d

db-stop: ## Stoppt PostgreSQL Container
	docker compose -f misc/docker-compose.yml down

db-clean: ## Stoppt PostgreSQL Container und löscht Daten
	docker compose -f misc/docker-compose.yml down -v

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
run-local: db-start ## Startet Anwendung mit lokaler DB und Ollama (docker-compose)
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

db-migrate: db-start ## Führt Flyway-Migrationen aus
	./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/pf2e_oracle -Dflyway.user=pf2e -Dflyway.password=pf2e

db-reset: db-clean db-migrate ## Setzt DB zurück und führt Migrationen aus
