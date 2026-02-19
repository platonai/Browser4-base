#!/usr/bin/env bash


BIN=$(dirname "$0")
APP_HOME=$(realpath "$BIN/..")
while [[ ! -f "$repoRoot/VERSION" && "$repoRoot" != "/" ]]; do
  APP_HOME=$(dirname "$repoRoot")
done
[[ -f "$repoRoot/VERSION" ]] || exit 1

MVNW="$repoRoot"/mvnw

"$BIN"/build.sh "$@"

SERVER_HOME=$repoRoot/browser4/browser4-agents
cd "$SERVER_HOME" || exit

"$BIN"/tools/install-depends.sh
"$MVNW" spring-boot:run

cd "$repoRoot" || exit
