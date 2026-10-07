APP_NAME=humanit-interview-app
API_DIR=api

.PHONY: run test integration-test verify build clean docker-build docker-run compile

run:
	cd $(API_DIR) && mvn spring-boot:run

test:
	cd $(API_DIR) && mvn test

integration-test:
	cd $(API_DIR) && mvn verify

verify:
	cd $(API_DIR) && mvn verify

compile:
	cd $(API_DIR) && mvn compile

build:
	cd $(API_DIR) && mvn clean package

clean:
	cd $(API_DIR) && mvn clean

docker-build:
	docker build -t $(APP_NAME) .

docker-run:
	docker run --rm -p 8080:8080 $(APP_NAME)
