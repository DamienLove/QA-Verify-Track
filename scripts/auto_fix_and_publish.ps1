$ErrorActionPreference = "Stop"

param(
    [string]$RepoOwner,
    [string]$RepoName,
    [string]$IssueNumber,
    [string]$Title,
    [string]$Description,
    [string]$BuildNumber,
    [string]$GithubToken
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

function Increment-AndroidVersion {
    $gradlePath = Join-Path $PSScriptRoot "..\androidApp\app\build.gradle.kts"
    if (-not (Test-Path $gradlePath)) {
        Write-Error "Android build.gradle.kts not found at $gradlePath"
        return $null
    }

    $gradleFile = Resolve-Path $gradlePath
    $content = Get-Content $gradleFile -Raw

    $versionCodePattern = 'versionCode\s*=\s*(\d+)'
    $versionNamePattern = 'versionName\s*=\s*"(\d+)"'

    $newVersion = 0

    if ($content -match $versionCodePattern) {
        $currentVersion = [int]$matches[1]
        $newVersion = $currentVersion + 1
        $content = $content -replace $versionCodePattern, "versionCode = $newVersion"
        Write-Host "Incrementing Android versionCode from $currentVersion to $newVersion"
    }

    if ($content -match $versionNamePattern) {
        $content = $content -replace $versionNamePattern, "versionName = ""$newVersion"""
    }

    Set-Content -Path $gradleFile -Value $content -NoNewline
    return $newVersion
}

function Post-GitHubComment {
    param(
        [string]$Token,
        [string]$Owner,
        [string]$Repo,
        [string]$Issue,
        [string]$Body
    )

    if ([string]::IsNullOrWhiteSpace($Token)) {
        Write-Warning "No GitHub token provided. Skipping comment."
        return
    }

    $url = "https://api.github.com/repos/$Owner/$Repo/issues/$Issue/comments"
    $bodyJson = @{ body = $Body } | ConvertTo-Json

    try {
        $headers = @{
            "Authorization" = "token $Token"
            "Accept"        = "application/vnd.github.v3+json"
            "User-Agent"    = "QAVerifyTrack-AutoFix"
        }

        $response = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $bodyJson -ContentType "application/json"
        Write-Host "Comment posted successfully: $Body"
    } catch {
        Write-Error "Failed to post comment to GitHub: $_"
    }
}

# 1. Run AI Fix
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

# 2. Increment Android Version
$newVersion = Increment-AndroidVersion

if (-not $newVersion) {
    Write-Warning "Could not increment version. Using provided build number for comment: $BuildNumber"
    $newVersion = $BuildNumber
}

# 3. Build and Publish Android App
$gradleDir = Join-Path $PSScriptRoot "..\androidApp"
if (Test-Path $gradleDir) {
    $gradleDir = Resolve-Path $gradleDir
    $gradlew = Join-Path $gradleDir "gradlew.bat"
    if (-not (Test-Path $gradlew)) {
        $gradlew = Join-Path $gradleDir "gradlew"
    }

    Write-Host "Publishing Android App (v$newVersion)..."
    Push-Location $gradleDir
    try {
        & $gradlew ":app:publishProdReleaseBundle"

        if ($env:QAVT_PROMOTE_TRACKS -eq "true") {
            Write-Host "Promoting to closed, open, and production tracks..."
            & $gradlew ":app:promoteProdReleaseArtifact" "--from-track" "internal" "--to-track" "closed"
            & $gradlew ":app:promoteProdReleaseArtifact" "--from-track" "closed" "--to-track" "open"
            & $gradlew ":app:promoteProdReleaseArtifact" "--from-track" "open" "--to-track" "production"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Warning "AndroidApp directory not found at $gradleDir"
}

# 4. Build and Deploy Web App
Write-Host "Building and Deploying Web App..."
$webDir = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $webDir
try {
    # Check for pnpm
    if (Get-Command "pnpm" -ErrorAction SilentlyContinue) {
        Write-Host "Using pnpm..."
        cmd /c pnpm install
        cmd /c pnpm build
    } elseif (Get-Command "npm" -ErrorAction SilentlyContinue) {
        Write-Host "Using npm..."
        cmd /c npm install
        cmd /c npm run build
    } else {
        Write-Error "Neither pnpm nor npm found."
    }

    if (Get-Command "firebase" -ErrorAction SilentlyContinue) {
        Write-Host "Deploying to Firebase Hosting..."
        cmd /c firebase deploy --only hosting
    } else {
        Write-Warning "Firebase CLI not found. Skipping deployment."
    }
} finally {
    Pop-Location
}

# 5. Build Plugin (Optional - rebuild plugin if it was changed)
Write-Host "Building Plugin..."
$pluginDir = Join-Path $PSScriptRoot "..\android-studio-plugin"
if (Test-Path $pluginDir) {
    $pluginDir = Resolve-Path $pluginDir
    $pluginGradlew = Join-Path $pluginDir "gradlew.bat"
    if (-not (Test-Path $pluginGradlew)) {
        $pluginGradlew = Join-Path $pluginDir "gradlew"
    }
    if (Test-Path $pluginGradlew) {
        Push-Location $pluginDir
        try {
            & $pluginGradlew buildPlugin
        } finally {
            Pop-Location
        }
    }
}

# 6. Post Comment
$commentBody = "verify fix v$newVersion"
Write-Host "Posting comment: $commentBody"
Post-GitHubComment -Token $GithubToken -Owner $RepoOwner -Repo $RepoName -Issue $IssueNumber -Body $commentBody

Write-Host "Auto-fix and publish sequence completed."
