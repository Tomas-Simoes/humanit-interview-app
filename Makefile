APP_NAME=humanit-interview-app
API_DIR=api
ENV_FILE=.env
LOAD_ENV=set -a; [ ! -f $(ENV_FILE) ] || . ./$(ENV_FILE); set +a;

.PHONY: run test integration-test verify build clean docker-build docker-run compile format format-check coverage

run:
	$(LOAD_ENV) cd $(API_DIR) && mvn spring-boot:run

test:
	$(LOAD_ENV) cd $(API_DIR) && mvn test

integration-test:
	$(LOAD_ENV) cd $(API_DIR) && mvn verify

verify:
	$(LOAD_ENV) cd $(API_DIR) && mvn verify

compile:
	$(LOAD_ENV) cd $(API_DIR) && mvn compile

format:
	$(LOAD_ENV) cd $(API_DIR) && mvn spotless:apply

format-check:
	$(LOAD_ENV) cd $(API_DIR) && mvn spotless:check

coverage:
	$(LOAD_ENV) cd $(API_DIR) && mvn verify

build:
	$(LOAD_ENV) cd $(API_DIR) && mvn clean package

clean:
	cd $(API_DIR) && mvn clean

docker-build:
	docker build -t $(APP_NAME) .

docker-run:
	docker run --rm --env-file $(ENV_FILE) -p 8080:8080 $(APP_NAME)
