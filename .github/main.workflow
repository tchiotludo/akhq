workflow "Build on push" {
  on = "push"
  resolves = [
    "HTTP client",
    "Build docker image",
  ]
}

action "debug" {
  uses = "actions/bin/sh@9d4ef995a71b0771f438dd7438851858f4a55d0c"
  args = ["ls -ltr"]
}

action "HTTP client" {
  uses = "swinton/httpie.action@02571a073b9aaf33930a18e697278d589a8051c1"
  args = ["GET", "https://ifconfig.co/"]
}

action "Show Environment" {
  uses = "docker://alpine"
  args = "printenv"
}


action "Tests" {
  uses = "docker://openjdk/8-jdk-alpine"
  needs = ["Show Environment"]
  runs = "./gradlew test"
}

action "Build Jar" {
  uses = "docker://openjdk/8-jdk-alpine"
  needs = ["Tests"]
  runs = "apk update && apk add --no-cache nodejs-npm && npm install && ./gradlew jar"
}

action "Build docker image" {
  uses = "actions/docker/cli@master"
  needs = ["Build Jar"]
  runs = "ls && ls build/libs/kafkahq-*.jar"
}
