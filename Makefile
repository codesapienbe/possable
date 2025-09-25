# Makefile for building and running the possable project
# Targets:
#   make build  	   -> build Docker image using Dockerfile.native
#   make run    	   -> run the docker image (exposes :8080)
#   make run-local     -> run the built jar locally
#   make clean         -> mvn clean + remove image/container/target

# Default image name and dockerfile (override with IMAGE_NAME or DOCKERFILE env var)
IMAGE_NAME ?= possable:latest
DOCKERFILE ?= Dockerfile.native
MVN ?= mvn -DskipTests
JAR_WILDCARD := $(wildcard target/*.jar)
# JAR resolution removed for docker-only workflow (run-local target removed)

.DEFAULT_GOAL := help

.PHONY: help build run clean

help:
	@echo "Usage: make <target>"
	@echo "  build          Build Docker image (uses $(DOCKERFILE))"
	@echo "  run            Run the docker image (exposes 8080)"
	@echo "  clean          Clean Maven artifacts, remove target and docker image/container"

build:
	@echo "Building docker image using $(DOCKERFILE)"
	docker build -f $(DOCKERFILE) -t $(IMAGE_NAME) .

# run-local target removed intentionally

run:
	@echo "Running docker image $(IMAGE_NAME)"
	docker run --rm -p 8080:8080 --name possable_app $(IMAGE_NAME)

clean:
	@echo "Cleaning maven artifacts, docker container and image, target directory"
	$(MVN) clean
	-@docker rm -f possable_app 2>/dev/null || true
	-@docker rmi $(IMAGE_NAME) 2>/dev/null || true
	-@rm -rf target
