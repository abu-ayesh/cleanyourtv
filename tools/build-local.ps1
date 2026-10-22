# Build only. No ADB, package changes, HOME changes or deployment.
param([string]$GradleUserHome='G:\Android\mihrab-gradle')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:GRADLE_USER_HOME=$GradleUserHome
Push-Location $projectRoot
try {
    # Windows PowerShell wraps javac's informational stderr as NativeCommandError.
    # Gradle's exit code is authoritative; keep warnings visible without aborting the process.
    $ErrorActionPreference='Continue'
    & .\gradlew.bat --no-daemon --project-cache-dir (Join-Path $GradleUserHome 'project-cache') testDebugUnitTest lintDebug assembleDebug --console=plain
    $buildExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($buildExit -ne 0){throw "Local Gradle build failed ($buildExit)"}
} finally { Pop-Location }
