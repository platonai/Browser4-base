# Build the MkDocs documentation site (PowerShell)
$ErrorActionPreference = 'Stop'

$docsDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $docsDir

Write-Host "Building Browser4 Python SDK documentation..."

# Install docs toolchain
python -m pip install -r requirements.txt

# Ensure the local SDK is importable for mkdocstrings (editable install)
python -m pip install -e ..

Write-Host "Building site..."
python -m mkdocs build

Write-Host "✓ Documentation built successfully!"
Write-Host "Site generated in: site/"
