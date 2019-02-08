workflow "Build on push" {
  on = "push"
  resolves = [
    "HTTP client",
    "debug",
    "Build docker image",
  ]
}

action "debug" {
  uses = "actions/bin/sh@latest"
  args = ["ls -ltr"]
}

action "HTTP client" {
  uses = "swinton/httpie.action@02571a073b9aaf33930a18e697278d589a8051c1"
  args = ["GET", "https://ifconfig.co/"]
}

action "Tests" {
  uses = "docker://openjdk:8-jdk-alpine:latest"
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
