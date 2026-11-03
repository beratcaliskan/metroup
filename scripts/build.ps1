$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
if (-not $env:JAVA_HOME) {
    $localJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'JDK 17 kur ve JAVA_HOME değişkenini ayarla.' }
# This Windows host rejects Java AF_UNIX loopback pipes. A non-existent socket
# directory makes the JDK fall back to TCP without changing system settings.
$previousBuildRoot = $env:METROUP_BUILD_ROOT
if (-not $env:METROUP_BUILD_ROOT) {
    $hasher = [System.Security.Cryptography.SHA256]::Create()
    try { $projectKey = ([BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectRoot)))).Replace('-', '').Substring(0, 12) }
    finally { $hasher.Dispose() }
    $env:METROUP_BUILD_ROOT = Join-Path $env:LOCALAPPDATA "MetroUp/build/$projectKey"
}
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
$socketFallback = Join-Path $projectRoot '.tools/no-unix-sockets'
$env:JAVA_TOOL_OPTIONS = "$previousJavaOptions `"-Djdk.net.unixdomain.tmpdir=$socketFallback`""
try {
& .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --init-script (Join-Path $PSScriptRoot 'local-build.init.gradle') --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Derleme veya kontroller başarısız.' }
$apkOutput = Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk'
New-Item -ItemType Directory -Force -Path (Split-Path $apkOutput -Parent) | Out-Null
Copy-Item -LiteralPath (Join-Path $env:METROUP_BUILD_ROOT 'app/outputs/apk/debug/app-debug.apk') -Destination $apkOutput -Force
Write-Output $apkOutput
Write-Output "Raporlar: $(Join-Path $env:METROUP_BUILD_ROOT 'app/reports')"
} finally {
    $env:JAVA_TOOL_OPTIONS = $previousJavaOptions
    $env:METROUP_BUILD_ROOT = $previousBuildRoot
}
