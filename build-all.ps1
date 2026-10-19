[CmdletBinding()]
param(
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$WorkspaceParent = Split-Path -Parent $Root.TrimEnd('\', '/')
$OutputDirectory = Join-Path $WorkspaceParent 'mods'

$OriginalJavaHome = $env:JAVA_HOME
$OriginalPath = $env:Path

function Use-JavaHome([string]$VariableName, [string]$Label) {
    $candidate = [Environment]::GetEnvironmentVariable($VariableName)

    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        if (-not (Test-Path (Join-Path $candidate 'bin\java.exe'))) {
            throw "$VariableName points to an invalid JDK directory: $candidate"
        }

        $env:JAVA_HOME = $candidate
        $env:Path = "$(Join-Path $candidate 'bin');$OriginalPath"
        Write-Host "Using $Label from ${VariableName}: $candidate"
        return
    }

    if (-not [string]::IsNullOrWhiteSpace($OriginalJavaHome)) {
        $env:JAVA_HOME = $OriginalJavaHome
        $env:Path = "$(Join-Path $OriginalJavaHome 'bin');$OriginalPath"
        Write-Host "${VariableName} is not set; using current JAVA_HOME for ${Label}: $OriginalJavaHome" -ForegroundColor Yellow
        return
    }

    $env:JAVA_HOME = $null
    $env:Path = $OriginalPath
    Write-Host "${VariableName} and JAVA_HOME are not set; Gradle will use java.exe from PATH for ${Label}." -ForegroundColor Yellow
}

function Invoke-Build(
    [string]$DisplayName,
    [string]$Folder,
    [string]$JavaVariable,
    [string]$JavaLabel
) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building $DisplayName" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    Use-JavaHome $JavaVariable $JavaLabel

    Push-Location (Join-Path $Root $Folder)
    try {
        if ($Clean) {
            cmd.exe /c "gradlew.bat clean build"
        } else {
            cmd.exe /c "gradlew.bat build"
        }
        if ($LASTEXITCODE -ne 0) {
            throw "$DisplayName build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

function Get-LatestReleaseJar(
    [string]$Folder,
    [string]$NamePattern,
    [string]$DisplayName
) {
    if (-not (Test-Path $Folder)) {
        throw "$DisplayName output directory does not exist: $Folder"
    }

    $jar = Get-ChildItem -Path $Folder -Filter $NamePattern -File |
        Where-Object {
            $_.Name -notmatch '(-sources|-dev|-javadoc|-slim|-plain)\.jar$'
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Could not find the release JAR for $DisplayName in: $Folder"
    }

    return $jar
}

function Copy-ReleaseJar(
    [System.IO.FileInfo]$Jar,
    [string]$DisplayName
) {
    $destination = Join-Path $OutputDirectory $Jar.Name
    Copy-Item -Path $Jar.FullName -Destination $destination -Force
    Write-Host "[COPIED] $DisplayName -> $destination" -ForegroundColor Green
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

try {
    Invoke-Build 'NeoForge 1.21.1'  'neoforge-1.21.1'  'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'Fabric 1.21.1'    'fabric-1.21.1'    'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'Fabric 1.21.11'   'fabric-1.21.11'   'JAVA21_HOME' 'JDK 21'

    $neoForge121Jar = Get-LatestReleaseJar `
        (Join-Path $Root 'neoforge-1.21.1\build\libs') `
        'vein_farming-1.21.1-neoforge-*.jar' `
        'NeoForge 1.21.1'

    $fabric121Jar = Get-LatestReleaseJar `
        (Join-Path $Root 'fabric-1.21.1\build\libs') `
        'vein_farming-fabric-1.21.1-*.jar' `
        'Fabric 1.21.1'

    $fabric12111Jar = Get-LatestReleaseJar `
        (Join-Path $Root 'fabric-1.21.11\build\libs') `
        'vein_farming-fabric-1.21.11-*.jar' `
        'Fabric 1.21.11'

    Write-Host ""
    Write-Host "Copying artifacts to release directory..." -ForegroundColor Cyan
    Copy-ReleaseJar $neoForge121Jar   'NeoForge 1.21.1'
    Copy-ReleaseJar $fabric121Jar     'Fabric 1.21.1'
    Copy-ReleaseJar $fabric12111Jar   'Fabric 1.21.11'

    Write-Host ""
    Write-Host "All 3 builds completed successfully!" -ForegroundColor Green
    Write-Host "Release directory: $OutputDirectory" -ForegroundColor Green
}
finally {
    $env:JAVA_HOME = $OriginalJavaHome
    $env:Path = $OriginalPath
}
