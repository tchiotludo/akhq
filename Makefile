.PHONY: docker build_image snyk

docker:
	@COMMIT_ID=$$(git rev-parse --short HEAD); \
	$(MAKE) build_image IMAGE_NAME=173672169127.dkr.ecr.us-east-1.amazonaws.com/devrev/akhq:stable; \
	docker tag 173672169127.dkr.ecr.us-east-1.amazonaws.com/devrev/akhq:stable 173672169127.dkr.ecr.us-east-1.amazonaws.com/devrev/akhq:v$$COMMIT_ID

build_image:
	@if [ -z "$(IMAGE_NAME)" ]; then \
		echo "Error: IMAGE_NAME is required. Usage: make build_image IMAGE_NAME=<image-name>"; \
		exit 1; \
	fi
	./gradlew shadowJar
	cp build/libs/akhq-*-all.jar docker/app/akhq.jar
	docker build -t $(IMAGE_NAME) -f Dockerfile.devrev .

snyk:
	@TMP_IMAGE="akhq-snyk-tmp-$$(date +%s)"; \
	$(MAKE) build_image IMAGE_NAME=$$TMP_IMAGE; \
	echo "Testing image: $$TMP_IMAGE"; \
	snyk container test $$TMP_IMAGE || EXIT_CODE=$$?; \
	docker rmi $$TMP_IMAGE || true; \
	exit $${EXIT_CODE:-0}

