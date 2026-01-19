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

function Resolve-AICommandTemplate {
    $aiTemplate = $env:QAVT_AI_COMMAND
    if (-not [string]::IsNullOrWhiteSpace($aiTemplate)) {
        return $aiTemplate
    }

    $provider = $env:QAVT_AI_PROVIDER
    if (-not [string]::IsNullOrWhiteSpace($provider)) {
        switch ($provider.ToLower()) {
            "claude" { return "claude ""{prompt}""" }
            "gemini" { return "gemini ""{prompt}""" }
            "codex" { return "codex ""{prompt}""" }
            "openai" { return "codex ""{prompt}""" }
        }
    }

    if (Get-Command codex -ErrorAction SilentlyContinue) { return "codex ""{prompt}""" }
    if (Get-Command claude -ErrorAction SilentlyContinue) { return "claude ""{prompt}""" }
    if (Get-Command gemini -ErrorAction SilentlyContinue) { return "gemini ""{prompt}""" }

    return "codex ""{prompt}"""
}

function Has-GitChanges {
    $status = git status --porcelain 2>$null
    return -not [string]::IsNullOrWhiteSpace($status)
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if ([string]::IsNullOrWhiteSpace($IssueUrl) -and -not [string]::IsNullOrWhiteSpace($RepoOwner) -and -not [string]::IsNullOrWhiteSpace($RepoName) -and -not [string]::IsNullOrWhiteSpace($IssueNumber)) {
    $IssueUrl = "https://github.com/$RepoOwner/$RepoName/issues/$IssueNumber"
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
Do not publish artifacts; only change code.

Issue: #$IssueNumber $Title
Repo: $RepoOwner/$RepoName
Build: $BuildNumber
URL: $IssueUrl

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
    issueUrl    = $IssueUrl
    prompt      = $prompt
}

$aiTemplate = Resolve-AICommandTemplate
$aiCommand = Expand-Template -Template $aiTemplate -Values $values
Write-Host "Running AI command: $aiCommand"
Push-Location $repoRoot
try {
    Invoke-Expression $aiCommand
} finally {
    Pop-Location
}

$requireChange = $env:QAVT_REQUIRE_CODE_CHANGE
if ($requireChange -ne "false") {
    if (-not (Has-GitChanges)) {
        Write-Error "No code changes detected. Auto-fix requires a code change."
        exit 2
    }
}

$commitChanges = $env:QAVT_COMMIT_CHANGES
if ($commitChanges -ne "false") {
    if (Get-Command git -ErrorAction SilentlyContinue) {
        Push-Location $repoRoot
        try {
            $insideRepo = git rev-parse --is-inside-work-tree 2>$null
            if ($LASTEXITCODE -eq 0 -and $insideRepo.Trim() -eq "true") {
                if (Has-GitChanges) {
                    git add -A | Out-Host
                    $cleanTitle = if ([string]::IsNullOrWhiteSpace($Title)) { "issue $IssueNumber" } else { $Title }
                    $commitMessage = "Auto-fix issue #$IssueNumber: $cleanTitle"
                    git commit -m $commitMessage | Out-Host
                    if ($env:QAVT_PUSH_CHANGES -eq "true") {
                        git push | Out-Host
                    } else {
                        Write-Host "Skipping git push. Set QAVT_PUSH_CHANGES=true to enable."
                    }
                } else {
                    Write-Host "No git changes detected; skipping commit."
                }
            } else {
                Write-Host "Not a git repository; skipping commit."
            }
        } catch {
            Write-Host "Failed to commit changes: $($_.Exception.Message)"
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "git not available; skipping commit."
    }
}

$publishAndroid = $env:QAVT_PUBLISH_ANDROID

# 2. Increment Android Version
$newVersion = Increment-AndroidVersion

if (-not $newVersion) {
    Write-Warning "Could not increment version. Using provided build number for comment: $BuildNumber"
    $newVersion = $BuildNumber
}

# 3. Build and Publish Android App
if ($publishAndroid -eq "false") {
    Write-Host "Skipping Play Store publish. Set QAVT_PUBLISH_ANDROID=true to enable."
} else {
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
}

$publishWeb = $env:QAVT_PUBLISH_WEB
if ($publishWeb -eq "false") {
    Write-Host "Skipping web deploy. Set QAVT_PUBLISH_WEB=true to enable."
} else {
    $webBuildCommand = $env:QAVT_WEB_BUILD_COMMAND
    if ([string]::IsNullOrWhiteSpace($webBuildCommand)) {
        $webBuildCommand = "npm run build"
    }
    $webDeployCommand = $env:QAVT_WEB_DEPLOY_COMMAND
    if ([string]::IsNullOrWhiteSpace($webDeployCommand)) {
        $webDeployCommand = "npm run deploy:hosting"
    }
    Push-Location $repoRoot
    try {
        Write-Host "Building web app..."
        Invoke-Expression $webBuildCommand
        Write-Host "Deploying web app..."
        Invoke-Expression $webDeployCommand
    } finally {
        Pop-Location
    }
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
$commentBody = "verify fixes $newVersion"
Write-Host "Posting comment: $commentBody"
Post-GitHubComment -Token $GithubToken -Owner $RepoOwner -Repo $RepoName -Issue $IssueNumber -Body $commentBody

Write-Host "Auto-fix and publish sequence completed."
