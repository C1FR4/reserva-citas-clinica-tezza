. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Preparar-Proyecto
    Solicitar-ClaveDb
    $env:MAIL_ENABLED='false'
    $env:CUENTA_ROL='ADMINISTRADOR'
    $env:CUENTA_USUARIO=Read-Host 'Usuario para el administrador (por ejemplo admin)'
    $env:CUENTA_NOMBRE=Read-Host 'Nombres del administrador'
    $env:CUENTA_APELLIDO=Read-Host 'Apellidos del administrador'
    $env:CUENTA_CORREO=Read-Host 'Correo de contacto del administrador'
    Solicitar-ClaveCuenta
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=cuenta-interna' '-Dspring-boot.run.arguments=--spring.main.web-application-type=none'
    if($LASTEXITCODE -ne 0){throw 'La cuenta no se pudo crear. Revisa el mensaje anterior.'}
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
finally { Remove-Item Env:CUENTA_PASSWORD -ErrorAction SilentlyContinue }
