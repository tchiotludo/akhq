workflow "Build on push" {
  on = "push"
  resolves = ["Tests"]
}

action "debug" {
  uses = "actions/bin/sh@latest"
  args = ["ls -ltr"]
}

action "Tests" {
  uses = "docker://openjdk:8-jdk-alpine:latest"
  needs = ["debug"]
  runs = "./gradlew test"
}

action "Build Jar" {
  uses = "docker://openjdk:8-jdk-alpine:latest"
  needs = ["Tests"]
  runs = "apk update && apk add --no-cache nodejs-npm && npm install && ./gradlew jar"
}

action "Build docker image" {
  uses = "actions/docker/cli@master"
  needs = ["Build Jar"]
  runs = "ls && ls build/libs/kafkahq-*.jar"
}
