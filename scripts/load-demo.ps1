$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    Get-Content -LiteralPath 'database/demo-data.sql' | docker compose exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
    if ($LASTEXITCODE -ne 0) { throw 'Could not load demo records. Check that the app is running.' }
    Write-Host 'Demo records are ready. Refresh the employee list.'
} finally {
    Pop-Location
}
