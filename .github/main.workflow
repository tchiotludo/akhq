workflow "Build on push" {
  on = "push"
  resolves = [
    "HTTP client",
    "debug",
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
