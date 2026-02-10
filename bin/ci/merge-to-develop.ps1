#!/usr/bin/env pwsh

# Merge the current branch into develop
$CurrentBranch = git rev-parse --abrev-ref HEAD
if ($CurrentBranch -eq "develop") {
    Write-Host "Already on develop branch. No merge needed."
    exit 0
}

Write-Host "Merging branch '$CurrentBranch' into 'develop'..."
git checkout develop
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to checkout develop branch."
    exit 1
}
git merge $CurrentBranch
if ($LASTEXITCODE -ne 0) {
    Write-Error "Merge failed. Please resolve conflicts and commit the merge."
    exit 1
}
Write-Host "Merge successful. You can now push the develop branch."

git push origin develop
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push develop branch. Please push manually."
    exit 1
}
Write-Host "Develop branch pushed successfully."

git checkout $CurrentBranch
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to switch back to original branch '$CurrentBranch'. Please switch manually."
    exit 1
}
Write-Host "Switched back to original branch '$CurrentBranch'."
