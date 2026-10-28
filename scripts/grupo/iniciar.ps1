. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Preparar-Proyecto
    Solicitar-ClaveDb
    Write-Host ('Abre http://127.0.0.1:'+$env:PORT+'/InicioSesion.html cuando aparezca Started ClinicaApplication.')
    Write-Host 'La primera ejecución descarga dependencias. Mantén esta ventana abierta; Ctrl+C detiene el servidor.'
    & .\mvnw.cmd spring-boot:run
    if($LASTEXITCODE -ne 0){throw 'El servidor no inició. Revisa el error mostrado y la guía.'}
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
