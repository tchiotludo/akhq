include Makefile.sidekick

.PHONY: docker build_image snyk

docker:
	$(MAKE) build_image IMAGE_NAME=akhq:build

build_image:
	@if [ -z "$(IMAGE_NAME)" ]; then \
		echo "Error: IMAGE_NAME is required. Usage: make build_image IMAGE_NAME=<image-name>"; \
		exit 1; \
	fi
	./gradlew shadowJar
	cp build/libs/akhq-*-all.jar docker/app/akhq.jar
	docker build -t $(IMAGE_NAME) -f Dockerfile.devrev .

snyk: .d.snyk
	@TMP_IMAGE="akhq-snyk-tmp-$$(date +%s)"; \
	$(MAKE) build_image IMAGE_NAME=$$TMP_IMAGE; \
	echo "Testing image: $$TMP_IMAGE"; \
	$(SNYK) container test $$TMP_IMAGE || EXIT_CODE=$$?; \
	docker rmi $$TMP_IMAGE || true; \
	exit $${EXIT_CODE:-0}

