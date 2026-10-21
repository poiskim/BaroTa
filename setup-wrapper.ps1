# One-time official Gradle Wrapper JAR setup (required before opening in Android Studio).
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$destination = Join-Path $projectRoot 'gradle\wrapper\gradle-wrapper.jar'
$expected = '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'
$uri = 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar'
if (-not (Test-Path $destination)) {
    Write-Host 'Downloading official Gradle 8.13 wrapper JAR...'
    Invoke-WebRequest -Uri $uri -OutFile $destination
}
$actual = (Get-FileHash -Path $destination -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) {
    Remove-Item $destination -ErrorAction SilentlyContinue
    throw "Gradle wrapper checksum mismatch: $actual"
}
Write-Host 'Verified official Gradle 8.13 wrapper. Open this project in Android Studio.'
