. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Preparar-Proyecto
    Solicitar-ClaveDb
    $env:MAIL_ENABLED='false'
    $env:CUENTA_ROL='MEDICO'
    $env:CUENTA_DNI=Read-Host 'DNI de 8 dígitos del médico ya registrado (ver tabla de médicos en la guía)'
    $env:CUENTA_USUARIO=Read-Host 'Usuario de acceso del médico (por ejemplo medicogrupo)'
    Solicitar-ClaveCuenta
    & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=cuenta-interna' '-Dspring-boot.run.arguments=--spring.main.web-application-type=none'
    if($LASTEXITCODE -ne 0){throw 'La cuenta no se pudo crear. Revisa el mensaje anterior.'}
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
finally { Remove-Item Env:CUENTA_PASSWORD -ErrorAction SilentlyContinue }
