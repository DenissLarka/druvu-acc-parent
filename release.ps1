param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

$ErrorActionPreference = "Stop"

# Validate version format (e.g., 1.0.0)
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Error "Invalid version format. Expected format: X.Y.Z (e.g., 1.0.0)"
    exit 1
}

Write-Host "Starting release process for version $Version" -ForegroundColor Cyan

# Step 1: Set release version
Write-Host "`n[1/5] Setting version to $Version..." -ForegroundColor Yellow
mvn-change-version.ps1 $Version
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to set version"
    exit 1
}

# Step 2: Build and verify
Write-Host "`n[2/5] Building and verifying..." -ForegroundColor Yellow
mvn clean verify
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed"
    exit 1
}

# Step 3: Commit version change
Write-Host "`n[3/5] Committing version change..." -ForegroundColor Yellow
git add pom.xml **/pom.xml
git commit -m "RELEASE v$Version"
git push
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push changes"
    exit 1
}

# Step 4: Create GitHub release
Write-Host "`n[4/5] Creating GitHub release v$Version..." -ForegroundColor Yellow
gh release create "v$Version" --title "Release v$Version" --notes "Release v$Version"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create GitHub release"
    exit 1
}

# Step 5: Prepare next snapshot version
$versionParts = $Version -split '\.'
$patchVersion = [int]$versionParts[2] + 1
$nextVersion = "$($versionParts[0]).$($versionParts[1]).$patchVersion-SNAPSHOT"

Write-Host "`n[5/5] Setting next development version to $nextVersion..." -ForegroundColor Yellow
mvn-change-version.ps1 $nextVersion
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to set next snapshot version"
    exit 1
}

git add pom.xml **/pom.xml
git commit -m "Prepare for next development iteration"
git push

Write-Host "`nRelease v$Version completed successfully!" -ForegroundColor Green
Write-Host "Next development version: $nextVersion" -ForegroundColor Green
Write-Host "`nGitHub Actions will now build and publish." -ForegroundColor Cyan
Write-Host "Check progress at: https://github.com/DenissLarka/druvu-acc-parent/actions" -ForegroundColor Cyan
