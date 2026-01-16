$ErrorActionPreference = "Stop"

param(
    [string]$RepoOwner,
    [string]$RepoName,
    [string]$IssueNumber,
    [string]$Title,
    [string]$Description,
    [string]$BuildNumber
)

function Expand-Template {
    param(
        [string]$Template,
        [hashtable]$Values
    )
    $result = $Template
    foreach ($key in $Values.Keys) {
        $token = "{${key}}"
        $result = $result.Replace($token, $Values[$key])
    }
    return $result
}

$prompt = @"
You are an AI coding agent. Fix the issue in the repository and leave the workspace ready to build.

Issue: #$IssueNumber $Title
Repo: $RepoOwner/$RepoName
Build: $BuildNumber

Description:
$Description
"@

$values = @{
    repoOwner   = $RepoOwner
    repoName    = $RepoName
    issueNumber = $IssueNumber
    title       = $Title
    description = $Description
    buildNumber = $BuildNumber
    prompt      = $prompt
}

$aiTemplate = $env:QAVT_AI_COMMAND
if ([string]::IsNullOrWhiteSpace($aiTemplate)) {
    $aiTemplate = "codex ""{prompt}"""
}

$aiCommand = Expand-Template -Template $aiTemplate -Values $values
Write-Host "Running AI command: $aiCommand"
Invoke-Expression $aiCommand

$gradleDir = Resolve-Path (Join-Path $PSScriptRoot "..\\androidApp")
$gradlew = Join-Path $gradleDir "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    $gradlew = Join-Path $gradleDir "gradlew"
}

Write-Host "Publishing AAB to internal track..."
Push-Location $gradleDir
try {
    & $gradlew ":app:publishProdReleaseBundle"

    if ($env:QAVT_PROMOTE_TRACKS -eq "true") {
        Write-Host "Promoting to closed, open, and production tracks..."
        & $gradlew ":app:promoteProdReleaseArtifact" "--from-track" "internal" "--to-track" "closed"
        & $gradlew ":app:promoteProdReleaseArtifact" "--from-track" "closed" "--to-track" "open"
        & $gradlew ":app:promoteProdReleaseArtifact" "--from-track" "open" "--to-track" "production"
    } else {
        Write-Host "Skipping track promotion. Set QAVT_PROMOTE_TRACKS=true to enable."
    }
} finally {
    Pop-Location
}
