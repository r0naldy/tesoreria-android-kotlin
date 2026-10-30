param(
    [switch]$Run,
    [string]$DeviceId
)
$ErrorActionPreference = 'Stop'
# Utiliza el JDK y el SDK configurados en el equipo de desarrollo.
Set-Location $PSScriptRoot
& .\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación' }
if ($Run) {
    $treasurySdk = $env:ANDROID_HOME
    if (-not $treasurySdk) { $treasurySdk = $env:ANDROID_SDK_ROOT }
    $treasuryAdb = if ($treasurySdk) { Join-Path $treasurySdk 'platform-tools\adb.exe' } else { 'adb' }
    $treasuryDeviceArgs = @()
    if ($DeviceId) { $treasuryDeviceArgs = @('-s', $DeviceId) }
    & $treasuryAdb @treasuryDeviceArgs install -r app\build\outputs\apk\debug\app-debug.apk
    if ($LASTEXITCODE -ne 0) { throw 'Falló la instalación; conecta un dispositivo o abre un emulador. Usa -DeviceId si hay varios.' }
    & $treasuryAdb @treasuryDeviceArgs shell am start -n pe.edu.hzg.tesoreria.debug/pe.edu.hzg.tesoreria.MainActivity
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir la aplicación en el dispositivo.' }
}
