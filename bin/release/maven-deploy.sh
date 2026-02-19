#bin


APP_HOME=$(cd "$(dirname "$0")">/dev/null || exit; pwd)
while [[ ! -f "$repoRoot/VERSION" && "$repoRoot" != "/" ]]; do
  APP_HOME=$(dirname "$repoRoot")
done
[[ -f "$repoRoot/VERSION" ]] && cd "$repoRoot" || exit

printUsage() {
  echo "Usage: deploy [-clean|-test]"
}

if [[ $# -gt 0 ]]; then
  echo printUsage
  exit 0
fi

ENABLE_TEST=false
CLEAN=false

while [[ $# -gt 0 ]]; do
  case $1 in
    -clean)
      CLEAN=true
      shift # past argument
      ;;
    -test)
      ENABLE_TEST=true
      shift # past argument
      ;;
    -h|-help|--help)
      printUsage
      exit 1
      ;;
    -*|--*)
      printUsage
      exit 1
      ;;
    *)
      shift # past argument
      ;;
  esac
done

echo "Deploy the project ..."
echo "Changing version ..."

SNAPSHOT_VERSION=$(head -n 1 "$repoRoot/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
echo "$VERSION" > "$repoRoot"/VERSION

find "$repoRoot" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$VERSION/" {} \;

if $CLEAN; then
  ./mvnw clean
fi

if $ENABLE_TEST; then
  ./mvnw --batch-mode deploy -P deploy,release
else
  ./mvnw --batch-mode deploy -P deploy,release -DskipTests
fi

exitCode=$?
[ $exitCode -eq 0 ] && echo "Build successfully" || exit 1

# Build browser4/browser4-agents/ but do not deploy the artifacts
echo "Building browser4/browser4-agents/ ..."
cd "$repoRoot/browser4/browser4-agents/" || exit
./mvnw install -DskipTests=true -Dmaven.javadoc.skip=true

exitCode=$?
[ $exitCode -eq 0 ] && echo "Build successfully" || exit 1

cd "$repoRoot" || exit

echo "Artifacts are uploaded, you should publish manually:"
echo "https://central.sonatype.com/publishing"
echo "Hit the following link to check if the artifacts are synchronized to the maven center: "
echo "https://repo1.maven.org/maven2/ai/platon/pulsar"
