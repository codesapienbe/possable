# Makefile for building and running the possable project
# Targets:
#   make build-local   -> build JVM jar with Maven
#   make build-docker  -> build Docker image using Dockerfile.native
#   make run-local     -> run the built jar locally
#   make run-docker    -> run the docker image (exposes :8080)
#   make clean         -> mvn clean + remove image/container/target

# Default image name and dockerfile (override with IMAGE_NAME or DOCKERFILE env var)
IMAGE_NAME ?= possable:latest
DOCKERFILE ?= Dockerfile.native
MVN ?= mvn -DskipTests
JAR := $(shell ls target/*.jar 2>/dev/null | head -n 1)

.DEFAULT_GOAL := help

.PHONY: help build-local build-docker run-local run-docker clean

help:
	@echo "Usage: make <target>"
	@echo ""
	@echo "Build targets:"
	@echo "  build-local    Build JVM jar locally (maven package)"
	@echo "  build-docker   Build Docker image (uses $(DOCKERFILE))"
	@echo ""
	@echo "Run targets:"
	@echo "  run-local      Run the built jar locally (java -jar target/*.jar)"
	@echo "  run-docker     Run the docker image (exposes 8080)"
	@echo ""
	@echo "Other:"
	@echo "  clean          Clean Maven artifacts, remove target and docker image/container"

build-local:
	@echo "Build native image using GraalVM native plugin (requires GraalVM/native-image)"
ifeq ($(OS),Windows_NT)
	@$(MVN) -Pnative package
else
	@$(MVN) -Pnative package
	@if [ -f "target/possable" ]; then chmod +x target/possable; fi
endif

build-docker:
	@echo "Building docker image using $(DOCKERFILE)"
	docker build -f $(DOCKERFILE) -t $(IMAGE_NAME) .

run-local:
	@echo "Running application locally (native if available, otherwise jar)"
ifeq ($(OS),Windows_NT)
	@powershell -NoProfile -Command "if (Test-Path 'target/possable') { $env:SPRING_AUTOCONFIGURE_EXCLUDE='org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration'; & 'target/possable' } elseif ('$(JAR)' -eq '') { Write-Host 'No runnable artifact found - run \"make build-local\" first'; exit 1 } else { & java -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration -jar '$(JAR)' }"
else
	@if [ -x "target/possable" ]; then \
		SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration ./target/possable; \
	elif [ -z "$(JAR)" ]; then \
		echo "No runnable artifact found - run 'make build-local' first"; exit 1; \
	else \
		java -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration -jar "$(JAR)"; \
	fi
endif

run-docker:
	@echo "Running docker image $(IMAGE_NAME)"
	docker run --rm -p 8080:8080 --name possable_app $(IMAGE_NAME)

clean:
	@echo "Cleaning maven artifacts, docker container and image, target directory"
	$(MVN) clean
	-@docker rm -f possable_app 2>/dev/null || true
	-@docker rmi $(IMAGE_NAME) 2>/dev/null || true
	-@rm -rf target
