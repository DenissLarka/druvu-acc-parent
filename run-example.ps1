#!/usr/bin/env pwsh
# Run AccApiReadExample with proper Java module environment
# Usage: ./run-example.ps1 <gnucash-file>

param(
	[Parameter(Mandatory = $true, Position = 0)]
	[string]$GnucashFile
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Verify file exists
if (-not (Test-Path $GnucashFile)) {
	Write-Host "Error: File not found: $GnucashFile" -ForegroundColor Red
	exit 1
}

# Build the project (package JARs)
Write-Host "Building project..." -ForegroundColor Cyan
mvn clean install -DskipTests -q
if ($LASTEXITCODE -ne 0) {
	Write-Host "Build failed!" -ForegroundColor Red
	exit 1
}

# Get dependency classpath to a temp file
Write-Host "Resolving dependencies..." -ForegroundColor Cyan
$tempFile = [System.IO.Path]::GetTempFileName()
mvn -pl druvu-acc-tests dependency:build-classpath "-Dmdep.outputFile=$tempFile" -DincludeScope=runtime -q
$depClasspath = Get-Content $tempFile -Raw
Remove-Item $tempFile

# Build module path using JARs
$modulePath = @(
	"druvu-acc-tests/target/druvu-acc-tests-1.0.0-SNAPSHOT.jar"
	$depClasspath.Trim()
) -join [System.IO.Path]::PathSeparator

# Run with module path
Write-Host "Running AccApiExample..." -ForegroundColor Cyan
#Write-Host ($modulePath -replace ':', "`n")
java --module-path $modulePath --module com.druvu.acc.examples/com.druvu.acc.example.AccApiReadExample $GnucashFile
