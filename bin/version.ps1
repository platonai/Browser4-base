#!/usr/bin/env pwsh

$repoRoot = (git rev-parse --show-toplevel 2>$null)
Set-Location $repoRoot

$VERSION = "v$(Get-Content "$repoRoot/VERSION")"

if ($args.Count -gt 0 -and $args[0] -eq "-v") {
    # dynamically pull more interesting stuff from latest git commit
    $HASH = (git show-ref --head --hash=7 head).Substring(0, 7)      # first 7 letters of hash should be enough; that's what GitHub uses
    $BRANCH = (git rev-parse --abbrev-ref HEAD)
    $DATE = (git log -1 --pretty=%ad --date=short)

    # Return the version string used to describe this version of Metabase.
    Write-Output "$VERSION $HASH $BRANCH $DATE"
} else {
    Write-Output $VERSION
}
