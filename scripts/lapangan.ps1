<#
Skrip hari lapangan. Tidak berisi rahasia; kunci dibaca dari backend/.env dan tidak pernah dicetak.
  .\scripts\lapangan.ps1 mulai    # backend asli (Gemini) di port 8080 + adb reverse
  .\scripts\lapangan.ps1 pasang   # build aplikasi debug yang menunjuk ke localhost:8080 lalu pasang ke HP
  .\scripts\lapangan.ps1 bersih   # cabut adb reverse dan hapus foto yang tertinggal di HP
#>
param([Parameter(Mandatory)][ValidateSet('mulai', 'pasang', 'bersih')][string]$Perintah)

$ErrorActionPreference = 'Stop'
$akar = Split-Path -Parent $PSScriptRoot
$paket = 'com.danibanan.notakulakan'

switch ($Perintah) {
    'mulai' {
        $env_ = Join-Path $akar 'backend\.env'
        if (-not (Test-Path $env_)) { throw 'backend/.env tidak ada (salin dari .env.example dan isi GEMINI_API_KEY)' }
        foreach ($baris in Get-Content $env_) {
            if ($baris -match '^\s*([A-Z_]+)\s*=\s*([^#]*?)\s*(#.*)?$') { Set-Item "env:$($Matches[1])" $Matches[2] }
        }
        if (-not $env:GEMINI_API_KEY) { throw 'GEMINI_API_KEY kosong di backend/.env' }
        $env:NOTA_KLIEN = 'gemini'
        $env:APP_CHECK = 'off'
        adb reverse tcp:8080 tcp:8080 | Out-Null
        Write-Host "Backend: model $env:NOTA_MODEL, App Check off, port 8080. Ctrl+C untuk berhenti."
        Set-Location (Join-Path $akar 'backend')
        & .\.venv\Scripts\python.exe -m uvicorn nota.api:app --port 8080 --host 127.0.0.1
    }
    'pasang' {
        Set-Location $akar
        ./gradlew installDebug "-Pnota.api.url.debug=http://localhost:8080/"
    }
    'bersih' {
        adb reverse --remove tcp:8080 2>$null
        adb shell pm clear $paket | Out-Null
        Write-Host "Data aplikasi dihapus. Hapus juga foto asli dari galeri HP (lihat docs/LAPANGAN.md)."
    }
}
