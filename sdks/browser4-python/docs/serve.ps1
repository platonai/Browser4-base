# Serve the MkDocs documentation site locally (PowerShell)
$ErrorActionPreference = 'Stop'

$docsDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $docsDir

Write-Host "Serving Browser4 Python SDK documentation..."

# Install docs toolchain
python -m pip install -r requirements.txt

# Ensure the local SDK is importable for mkdocstrings (editable install)
python -m pip install -e ..

Write-Host "Starting local server..."
Write-Host "Documentation will be available at: http://127.0.0.1:8000"
Write-Host "Press Ctrl+C to stop the server"
Write-Host ""

python -m mkdocs serve
