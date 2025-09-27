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

.PHONY: help build run clean dev stop

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
	@echo "Running application locally"
	mvn -Pnative -DskipTests clean package
	@echo "Starting application in background and writing PID to .dev_pid"
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -Command "Start-Process -FilePath 'mvn' -ArgumentList '-Pnative','-DskipTests','spring-boot:run' -PassThru | Select-Object -ExpandProperty Id | Out-File -FilePath '.dev_pid' -Encoding ascii"
else
	@bash -c 'mvn -Pnative -DskipTests spring-boot:run > /dev/null 2>&1 & echo $$! > .dev_pid' || { nohup mvn -Pnative -DskipTests spring-boot:run > /dev/null 2>&1 & echo $$! > .dev_pid; }
endif

ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -Command "Start-Process 'http://localhost:8080'"
else
	@sleep 2; \
	if command -v xdg-open >/dev/null 2>&1; then xdg-open http://localhost:8080; \
	elif command -v open >/dev/null 2>&1; then open http://localhost:8080; \
	else echo "Please open http://localhost:8080 in your browser"; fi
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

stop:
	@echo "Stopping dev server if running (uses .dev_pid)"
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -Command "if (Test-Path -Path '.dev_pid') { $pid = Get-Content -Path '.dev_pid' ; if (Get-Process -Id $pid -ErrorAction SilentlyContinue) { Stop-Process -Id $pid -Force } ; Remove-Item -Path '.dev_pid' -ErrorAction SilentlyContinue } else { Write-Host 'No .dev_pid file found' }"
else
	@if [ -f .dev_pid ]; then PID=$$(cat .dev_pid); \
		if kill -0 $$PID >/dev/null 2>&1; then echo "Stopping process $$PID"; kill $$PID && rm -f .dev_pid || echo "Failed to stop process $$PID"; \
		else echo "No process $$PID running, removing stale .dev_pid"; rm -f .dev_pid; fi; \
	else echo "No .dev_pid file found"; fi
endif
