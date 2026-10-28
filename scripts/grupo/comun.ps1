$ErrorActionPreference='Stop'

function Preparar-Proyecto {
    $raiz=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    Set-Location -LiteralPath $raiz
    $archivo=Join-Path $raiz '.env.grupo.local'
    if(-not (Test-Path -LiteralPath $archivo)){throw 'Falta .env.grupo.local en la carpeta del proyecto.'}
    foreach($linea in Get-Content -LiteralPath $archivo -Encoding UTF8){
        $texto=$linea.Trim()
        if(-not $texto -or $texto.StartsWith('#')){continue}
        $par=$texto.Split([char[]]'=',2,[System.StringSplitOptions]::None)
        if($par.Count -ne 2 -or $par[0].Trim() -notmatch '^[A-Z][A-Z0-9_]*$'){throw 'Línea no válida en .env.grupo.local'}
        [Environment]::SetEnvironmentVariable($par[0].Trim(),$par[1].Trim().Trim('"').Trim("'"),'Process')
    }
    $candidatos=@()
    if($env:JAVA_HOME){$candidatos+=Join-Path $env:JAVA_HOME 'bin/java.exe'}
    $comando=Get-Command java -ErrorAction SilentlyContinue
    if($comando){$candidatos+=$comando.Source}
    foreach($base in @('C:\Program Files\Java','C:\Program Files\Eclipse Adoptium','C:\Program Files\Microsoft')){
        if(Test-Path -LiteralPath $base){
            foreach($carpeta in Get-ChildItem -LiteralPath $base -Directory){$candidatos+=Join-Path $carpeta.FullName 'bin/java.exe'}
        }
    }
    $encontrado=$false
    foreach($ejecutable in ($candidatos | Select-Object -Unique)){
        if(-not (Test-Path -LiteralPath $ejecutable)){continue}
        $version=(& $ejecutable --version 2>&1 | Out-String)
        if($version -match '(?:openjdk|java)\s+(\d+)' -and [int]$Matches[1] -ge 21){
            $jdk=Split-Path -Parent (Split-Path -Parent $ejecutable)
            if(-not (Test-Path -LiteralPath (Join-Path $jdk 'bin/java.exe'))){continue}
            $env:JAVA_HOME=$jdk
            $env:Path=(Join-Path $env:JAVA_HOME 'bin')+';'+$env:Path
            $encontrado=$true
            break
        }
    }
    if(-not $encontrado){throw 'Instala JDK 21 o superior y configura JAVA_HOME. No basta Java 17.'}
}

function Solicitar-ClaveDb {
    if(-not $env:DB_PASSWORD){
        $clave=Read-Host 'Contraseña de tu usuario MySQL (Enter si no tiene)' -AsSecureString
        $env:DB_PASSWORD=[System.Net.NetworkCredential]::new('', $clave).Password
    }
}

function Solicitar-ClaveCuenta {
    $clave=Read-Host 'Contraseña nueva: 8+, minúscula, mayúscula, número y símbolo, sin espacios' -AsSecureString
    $confirmacion=Read-Host 'Repite la contraseña nueva' -AsSecureString
    $texto=[System.Net.NetworkCredential]::new('', $clave).Password
    if($texto -ne [System.Net.NetworkCredential]::new('', $confirmacion).Password){throw 'Las contraseñas no coinciden.'}
    $env:CUENTA_PASSWORD=$texto
}
