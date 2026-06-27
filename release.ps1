<#
.SYNOPSIS
  Build a signed release and publish it as a GitHub release so the app can auto-update.

.EXAMPLE
  .\release.ps1 -VersionName 1.2 -Notes "Added reminders before each block."

  Increments APP_VERSION_CODE, sets APP_VERSION_NAME, builds the signed APK, writes
  version.json, and creates a GitHub release (tag vX.Y) with both files attached.
  The app checks releases/latest, so the newest release wins automatically.

.PARAMETER NoBump
  Reuse the current APP_VERSION_CODE instead of incrementing (used for the very first release).
#>
param(
    [Parameter(Mandatory = $true)][string]$VersionName,
    [string]$Notes = "Bug fixes and improvements.",
    [switch]$NoBump
)
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\bstmp"

# --- bump version in gradle.properties ---
$gp = Join-Path $root "gradle.properties"
$lines = Get-Content $gp
$code = [int](($lines | Select-String '^APP_VERSION_CODE=(.*)$').Matches.Groups[1].Value)
if (-not $NoBump) { $code = $code + 1 }
$lines = $lines -replace '^APP_VERSION_CODE=.*', "APP_VERSION_CODE=$code"
$lines = $lines -replace '^APP_VERSION_NAME=.*', "APP_VERSION_NAME=$VersionName"
Set-Content -Path $gp -Value $lines -Encoding ascii
Write-Host "Building versionCode=$code versionName=$VersionName ..."

# --- build signed APK ---
& "$root\gradlew.bat" assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }
$apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apk)) { throw "APK not found at $apk" }

# --- write version.json (published as a release asset) ---
$manifest = [ordered]@{
    versionCode = $code
    versionName = $VersionName
    notes       = $Notes
    apkUrl      = "https://github.com/thedickestrick/block-schedule/releases/latest/download/app-release.apk"
}
$vjson = Join-Path $root "version.json"
($manifest | ConvertTo-Json) | Set-Content -Path $vjson -Encoding ascii

# --- publish GitHub release (newest = "latest") ---
gh release create "v$VersionName" $apk $vjson --title "v$VersionName" --notes $Notes
if ($LASTEXITCODE -ne 0) { throw "gh release create failed" }

Write-Host "Published v$VersionName (code $code). The app will offer it on next launch / check."
