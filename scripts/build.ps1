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
$previousJavaOptions = $env:JAVA_TOOL_OPTIONS
$socketFallback = Join-Path $projectRoot '.tools/no-unix-sockets'
$env:JAVA_TOOL_OPTIONS = "$previousJavaOptions `"-Djdk.net.unixdomain.tmpdir=$socketFallback`""
try {
& .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Derleme veya kontroller başarısız.' }
Write-Output (Join-Path $projectRoot 'app/build/outputs/apk/debug/app-debug.apk')
} finally { $env:JAVA_TOOL_OPTIONS = $previousJavaOptions }
