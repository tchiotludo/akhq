workflow "Build on push" {
  on = "push"
  resolves = ["debug"]
}

action "debug" {
  uses = "actions/bin/sh@latest"
  args = ["ls -ltr"]
}
