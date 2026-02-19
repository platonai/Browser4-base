#!/bin/bash

# Find the first parent directory that contains a pom.xml file
APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ "$repoRoot" != "/" ]]; do
  if [[ -f "$repoRoot/pom.xml" ]]; then
    break
  fi
  APP_HOME=$(dirname "$repoRoot")
done

cd "$repoRoot" || exit

dos2unix $@ "$repoRoot"/mvnw

find "$repoRoot"/bin -type f -name "*.sh" -print0 | xargs -0 dos2unix $@
dos2unix $@ "$repoRoot"/VERSION

# find all bash files and add executable permission
find "$repoRoot"/bin -type f -name "*.sh" -print0 | xargs -0 chmod +x
