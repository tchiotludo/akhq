workflow "Build on push" {
  on = "push"
  resolves = [
    "Build docker image",
  ]
}

action "Tests" {
  uses = "docker://openjdk:8-jdk-alpine"
  runs = "./gradlew test"
}

action "Build Jar" {
  uses = "docker://openjdk:8-jdk-alpine"
  needs = ["Tests"]
  runs = "apk update && apk add --no-cache nodejs-npm && npm install && ./gradlew jar"
}

action "Build docker image" {
  uses = "actions/docker/cli@master"
  needs = ["Build Jar"]
  runs = "ls && ls build/libs/kafkahq-*.jar"
}
