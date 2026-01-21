# compact-vhdx-monthly.ps1
#
# PowerShell script for monthly VHDX compaction
# Reclaims disk space by compacting the WSL virtual disk
#
# Usage: .\scripts\compact-vhdx-monthly.ps1 [-Force] [-Verbose]
#
# Parameters:
#   -Force   : Skip confirmation prompts
#   -Verbose : Show detailed output
#
# Notes:
# - Requires Admin privileges
# - This process can take 10-30 minutes depending on disk size
# - Docker must be shut down during compaction
# - Can reclaim 50-200GB depending on docker system prune usage

param(
    [switch]$Force = $false,
    [switch]$Verbose = $false
)

# Colors and formatting
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Warning { Write-Host $args -ForegroundColor Yellow }
function Write-Error { Write-Host $args -ForegroundColor Red }
function Write-Info { Write-Host $args -ForegroundColor Cyan }
function Write-Verbose { if ($Verbose) { Write-Host $args -ForegroundColor Gray } }

Write-Info "================================"
Write-Info "VHDX Monthly Compaction"
Write-Info "================================"
Write-Info ""

# Check if running as admin
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error "ERROR: This script must be run as Administrator"
    Write-Info "Right-click PowerShell and select 'Run as Administrator'"
    exit 1
}

Write-Success "✓ Administrator privileges confirmed"
Write-Info ""

# Locate VHDX files
Write-Info "Locating WSL virtual disk files..."

$wslPath = "$env:LOCALAPPDATA\Packages\CanonicalGroupLimited.Ubuntu22.04LTS_79rhkp1fndgsc\LocalState"
$dockerPath = "$env:APPDATA\Docker\wsl\disk"

$vhdxFiles = @()

# Ubuntu WSL disk
if (Test-Path "$wslPath\ext4.vhdx") {
    $vhdxFiles += @{ Path = "$wslPath\ext4.vhdx"; Name = "Ubuntu-22.04 WSL Disk" }
    Write-Info "Found Ubuntu disk"
}

# Docker Desktop disk
if (Test-Path "$dockerPath\docker_data.vhdx") {
    $vhdxFiles += @{ Path = "$dockerPath\docker_data.vhdx"; Name = "Docker Desktop Data" }
    Write-Info "Found Docker Desktop disk"
}

if ($vhdxFiles.Count -eq 0) {
    Write-Error "ERROR: No VHDX files found"
    Write-Info "Checked paths:"
    Write-Info "  - $wslPath\ext4.vhdx"
    Write-Info "  - $dockerPath\docker_data.vhdx"
    exit 1
}

Write-Success "Found $($vhdxFiles.Count) VHDX file(s)"
Write-Info ""

# Show current sizes
Write-Info "Current VHDX sizes:"
foreach ($vhdx in $vhdxFiles) {
    $size = (Get-Item $vhdx.Path -ErrorAction SilentlyContinue).Length
    $sizeMB = [math]::Round($size / 1MB, 2)
    $sizeGB = [math]::Round($size / 1GB, 2)
    Write-Info "  $($vhdx.Name): $sizeGB GB ($sizeMB MB)"
}
Write-Info ""

# Confirmation
if (-not $Force) {
    Write-Warning "IMPORTANT:"
    Write-Warning "1. Docker Desktop MUST be stopped before compaction"
    Write-Warning "2. This process may take 10-30 minutes"
    Write-Warning "3. Do NOT interrupt the process"
    Write-Warning "4. You can reclaim 50-200GB depending on cleanup"
    Write-Warning ""

    $response = Read-Host "Continue with compaction? (yes/no)"
    if ($response -ne "yes") {
        Write-Info "Compaction cancelled"
        exit 0
    }
}

Write-Info ""

# Stop Docker Desktop
Write-Info "Stopping Docker Desktop..."
$dockerProcess = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
if ($dockerProcess) {
    Write-Info "Stopping Docker Desktop process..."
    Stop-Process -Name "Docker Desktop" -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    Write-Success "✓ Docker Desktop stopped"
} else {
    Write-Info "Docker Desktop is not running"
}

# Stop WSL
Write-Info "Shutting down WSL2..."
$wslProcess = Get-Process -Name "wsl" -ErrorAction SilentlyContinue
if ($wslProcess) {
    & wsl --shutdown 2>$null
    Write-Success "✓ WSL shutdown"
} else {
    Write-Info "WSL is not running"
}

Start-Sleep -Seconds 2
Write-Info ""

# Compact VHDX files
foreach ($vhdx in $vhdxFiles) {
    $path = $vhdx.Path
    $name = $vhdx.Name

    Write-Info "Compacting: $name"
    Write-Info "Path: $path"

    $beforeSize = (Get-Item $path).Length
    $beforeGB = [math]::Round($beforeSize / 1GB, 2)
    Write-Verbose "Before: $beforeGB GB"

    try {
        Write-Info "This may take several minutes..."

        # Use full Optimize-VHD for maximum compression
        Optimize-VHD -Path $path -Mode Full

        Start-Sleep -Seconds 2

        $afterSize = (Get-Item $path).Length
        $afterGB = [math]::Round($afterSize / 1GB, 2)
        $recoveredMB = [math]::Round(($beforeSize - $afterSize) / 1MB, 2)
        $recoveredGB = [math]::Round(($beforeSize - $afterSize) / 1GB, 2)

        Write-Verbose "After: $afterGB GB"
        Write-Success "✓ Compacted: $name"
        Write-Success "  Recovered: $recoveredGB GB ($recoveredMB MB)"
    } catch {
        Write-Error "✗ Failed to compact: $name"
        Write-Error "  Error: $_"
        continue
    }

    Write-Info ""
}

# Restart Docker Desktop
Write-Info "Restarting Docker Desktop..."
if (Test-Path "$env:APPDATA\Docker\Docker Desktop.exe") {
    & "$env:APPDATA\Docker\Docker Desktop.exe" 2>$null &
    Write-Info "Docker Desktop is starting..."
    Write-Info "(This may take 30-60 seconds)"
} else {
    Write-Warning "Could not find Docker Desktop executable"
    Write-Info "Please start Docker Desktop manually"
}

Write-Info ""

# Summary
Write-Info "================================"
Write-Success "✓ VHDX Compaction Complete!"
Write-Info "================================"
Write-Info ""
Write-Info "Next steps:"
Write-Info "1. Wait for Docker Desktop to fully start"
Write-Info "2. Verify with: docker ps"
Write-Info "3. Check reclaimed space: df -h"
Write-Info ""
Write-Info "Schedule this to run monthly to prevent disk space issues"
Write-Info ""
