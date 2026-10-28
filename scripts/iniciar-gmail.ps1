$ErrorActionPreference='Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$datos=Join-Path (Get-Location) '_trabajo_apf3'
if(-not $env:DB_PASSWORD){
    $local=Join-Path (Get-Location) '_trabajo_apf3/mysql/db_password.local'
    if(Test-Path -LiteralPath $local){$env:DB_PASSWORD=(Get-Content -Raw -LiteralPath $local).Trim()}
    else {
        $claveDb=Read-Host 'Contraseña de la base de datos de esta copia' -AsSecureString
        $env:DB_PASSWORD=[System.Net.NetworkCredential]::new('', $claveDb).Password
    }
}
$usuarioLocal=Join-Path $datos 'gmail_usuario.local'
$claveLocal=Join-Path $datos 'gmail_password.local'
if((Test-Path -LiteralPath $usuarioLocal) -and (Test-Path -LiteralPath $claveLocal)){
    $env:MAIL_USERNAME=(Get-Content -Raw -LiteralPath $usuarioLocal).Trim()
    $claveCorreo=(Get-Content -Raw -LiteralPath $claveLocal).Trim() | ConvertTo-SecureString
} else {
    $env:MAIL_USERNAME=Read-Host 'Correo Gmail remitente'
    $claveCorreo=Read-Host 'Contraseña de aplicación de Google (no la contraseña habitual)' -AsSecureString
}
$env:MAIL_PASSWORD=([System.Net.NetworkCredential]::new('', $claveCorreo).Password).Replace(' ','')
$env:MAIL_FROM="Clinica Tezza <$env:MAIL_USERNAME>"
$env:MAIL_HOST='smtp.gmail.com'
$env:MAIL_PORT='587'
$env:MAIL_AUTH='true'
$env:MAIL_STARTTLS='true'
$env:MAIL_ENABLED='true'
$env:MAIL_INTERVAL_MS='30000'
$env:DB_HOST='127.0.0.1'
$env:DB_PORT='3308'
$env:DB_NAME='clinica_tezza_recordatorios'
$env:DB_USER='clinica_app'
$env:PORT='8081'
try {
    $gestion=Join-Path $datos 'procesos.local'
    $actual=if(Test-Path -LiteralPath $gestion){Get-Content -Raw -LiteralPath $gestion | ConvertFrom-Json}else{$null}
    $puerto=Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue
    if($puerto){
        if(-not $actual -or $puerto.OwningProcess -ne $actual.app){throw '8081 está ocupado por otro proceso; no se detiene.'}
        $proc=Get-CimInstance Win32_Process -Filter ('ProcessId='+$actual.app)
        if($proc.CommandLine -notlike '*clinica-0.0.1-SNAPSHOT.jar*'){throw 'El proceso no corresponde a esta aplicación.'}
        Stop-Process -Id $actual.app
        Wait-Process -Id $actual.app -ErrorAction SilentlyContinue
    }
    $java=if(Test-Path -LiteralPath 'C:\Program Files\Java\jdk-26\bin\java.exe'){'C:\Program Files\Java\jdk-26\bin\java.exe'}
        elseif($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME/bin/java.exe")){"$env:JAVA_HOME/bin/java.exe"}
        else{(Get-Command java).Source}
    $proceso=Start-Process -FilePath $java -ArgumentList @('-jar','target/clinica-0.0.1-SNAPSHOT.jar','--spring.mail.test-connection=true') -WorkingDirectory (Get-Location).Path -WindowStyle Hidden -RedirectStandardOutput (Join-Path $datos 'gmail-servidor.log') -RedirectStandardError (Join-Path $datos 'gmail-servidor-error.log') -PassThru
    @{app=$proceso.Id;smtp=if($actual){$actual.smtp}else{$null};modo='GMAIL'} | ConvertTo-Json | Set-Content -LiteralPath $gestion
    Write-Output ('Copia iniciada con Gmail. PID '+$proceso.Id+'. URL: http://127.0.0.1:8081/InicioSesion.html')
}
finally { Remove-Item Env:MAIL_PASSWORD -ErrorAction SilentlyContinue }
