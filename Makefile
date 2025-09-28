# Makefile for building and running the possable project
# Targets:
#   make build  	   -> build Docker image using Dockerfile.native
#   make run    	   -> run the docker image (exposes :8080)
#   make dev           -> build native image locally and run it locally
#   make clean         -> mvn clean + remove image/container/target

# Default image name and dockerfile (override with IMAGE_NAME or DOCKERFILE env var)
IMAGE_NAME ?= possable:latest
DOCKERFILE ?= Dockerfile.native
MVN ?= mvn -DskipTests
JAR_WILDCARD := $(wildcard target/*.jar)
# JAR resolution removed for docker-only workflow (run-local target removed)

.DEFAULT_GOAL := help

.PHONY: help build run clean dev

help:
	@echo "Usage: make <target>"
	@echo "  build          Build Docker image (uses $(DOCKERFILE))"
	@echo "  run            Run the docker image (exposes 8080)"
	@echo "  clean          Clean Maven artifacts, remove target and docker image/container"
	@echo "  dev            Build native image locally and run it locally"

build:
	@echo "Building docker image using $(DOCKERFILE)"
	docker build -f $(DOCKERFILE) -t $(IMAGE_NAME) .

dev:
	@echo "Running application locally (background, logs -> .dev.log)"
ifeq ($(OS),Windows_NT)
	@pwsh -NoProfile -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c mvn spring-boot:run > .dev.log 2>&1' -WindowStyle Hidden"
	@pwsh -NoProfile -Command "$$timeout=30; $$i=0; $$port=$$null; while ($$i -lt $$timeout) { if (Test-Path '.dev.log') { $$content = (Get-Content -Path .dev.log -Tail 50) -join \"`n\"; if ($$content -match 'port\(s\):\s*([0-9]+)') { $$port = $$matches[1]; Write-Host \"Application running on port: $$($$port)\"; Start-Sleep -Seconds 8; Start-Process \"http://localhost:$$($$port)\"; exit 0 } if ($$content -match 'Started .* on port\s*([0-9]+)') { $$port = $$matches[1]; Write-Host \"Application running on port: $$($$port)\"; Start-Sleep -Seconds 8; Start-Process \"http://localhost:$$($$port)\"; exit 0 } if ($$content -match 'Tomcat started on port\s*([0-9]+)') { $$port = $$matches[1]; Write-Host \"Application running on port: $$($$port)\"; Start-Sleep -Seconds 8; Start-Process \"http://localhost:$$($$port)\"; exit 0 } } Start-Sleep -Seconds 1; $$i++ } Write-Host 'Could not determine application port from .dev.log'; Get-Content .dev.log -Tail 50 | Write-Host"
else
	@nohup mvn -DskipTests spring-boot:run > .dev.log 2>&1 &
	@( \
		timeout=30; i=0; port=""; \
		while [ $$i -lt $$timeout ]; do \
			if tail -n 50 .dev.log | grep -Eo 'port\(s\):[[:space:]]*[0-9]+' >/dev/null 2>&1; then \
				port=$$(tail -n 50 .dev.log | grep -Eo 'port\(s\):[[:space:]]*[0-9]+' | grep -Eo '[0-9]+' -m1); break; \
			fi; \
			if tail -n 50 .dev.log | grep -Eo 'Started .* on port[[:space:]]*[0-9]+' >/dev/null 2>&1; then \
				port=$$(tail -n 50 .dev.log | grep -Eo 'Started .* on port[[:space:]]*[0-9]+' | grep -Eo '[0-9]+' -m1); break; \
			fi; \
			if tail -n 50 .dev.log | grep -Eo 'Tomcat started on port[[:space:]]*[0-9]+' >/dev/null 2>&1; then \
				port=$$(tail -n 50 .dev.log | grep -Eo 'Tomcat started on port[[:space:]]*[0-9]+' | grep -Eo '[0-9]+' -m1); break; \
			fi; \
			sleep 1; i=$$((i+1)); \
		done; \
		if [ -n "$$port" ]; then printf "Application running on port: %s\n" "$$port"; \
			# wait briefly for the server to be ready before opening browser
			sleep 8; \
			# try to open the detected URL in the user's default browser
			if command -v xdg-open >/dev/null 2>&1; then xdg-open "http://localhost:$$port" >/dev/null 2>&1 || true; \
			elif command -v open >/dev/null 2>&1; then open "http://localhost:$$port" >/dev/null 2>&1 || true; \
			elif command -v python3 >/dev/null 2>&1; then python3 -m webbrowser "http://localhost:$$port" >/dev/null 2>&1 || true; \
			elif command -v python >/dev/null 2>&1; then python -m webbrowser "http://localhost:$$port" >/dev/null 2>&1 || true; \
			else printf "Open your browser at: http://localhost:%s\n" "$$port"; fi; \
		else printf "Could not determine application port from .dev.log within %s seconds. Tail last 50 lines:\n" "$$timeout"; tail -n 50 .dev.log; fi; \
	)
endif

run:
	@echo "Running docker image $(IMAGE_NAME)"
	docker run --rm -p 8080:8080 --name possable_app $(IMAGE_NAME)

clean:
	@echo "Cleaning maven artifacts, docker container and image, target directory"
	$(MVN) clean
	-@docker rm -f possable_app 2>/dev/null || true
	-@docker rmi $(IMAGE_NAME) 2>/dev/null || true
	-@rm -rf target

