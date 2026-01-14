.PHONY: build clean compile test run package help

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
