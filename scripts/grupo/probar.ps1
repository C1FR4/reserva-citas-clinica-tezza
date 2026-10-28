. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Preparar-Proyecto
    $env:MAIL_ENABLED='false'
    & .\mvnw.cmd test
    if($LASTEXITCODE -ne 0){throw 'Hay pruebas fallidas. Revisa el resultado anterior.'}
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
