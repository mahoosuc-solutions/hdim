# WSL Disk Compaction Script
# Run this from PowerShell as Administrator

Write-Host "=== WSL Disk Compaction Script ===" -ForegroundColor Cyan
Write-Host ""

# Check for Administrator privileges
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "ERROR: This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    exit 1
}

# Define disk paths
$dockerVhdx = "$env:LOCALAPPDATA\Docker\wsl\disk\docker_data.vhdx"
$ubuntuVhdx = "$env:LOCALAPPDATA\Packages\CanonicalGroupLimited.Ubuntu22.04LTS_79rhkp1fndgsc\LocalState\ext4.vhdx"

# Get initial sizes
Write-Host "Current disk sizes:" -ForegroundColor Yellow
if (Test-Path $dockerVhdx) {
    $dockerSize = (Get-Item $dockerVhdx).Length / 1GB
    Write-Host "  Docker VHDX: $([math]::Round($dockerSize, 2)) GB"
}
if (Test-Path $ubuntuVhdx) {
    $ubuntuSize = (Get-Item $ubuntuVhdx).Length / 1GB
    Write-Host "  Ubuntu VHDX: $([math]::Round($ubuntuSize, 2)) GB"
}
Write-Host ""

# Shutdown WSL
Write-Host "Step 1: Shutting down WSL..." -ForegroundColor Green
wsl --shutdown
Start-Sleep -Seconds 5

# Function to compact a VHDX file
function Compact-VhdxFile {
    param([string]$Path, [string]$Name)

    if (Test-Path $Path) {
        Write-Host "Step: Compacting $Name..." -ForegroundColor Green
        $sizeBefore = (Get-Item $Path).Length / 1GB

        # Create diskpart script
        $diskpartScript = @"
select vdisk file="$Path"
attach vdisk readonly
compact vdisk
detach vdisk
"@
        $scriptPath = "$env:TEMP\compact_$Name.txt"
        $diskpartScript | Out-File -FilePath $scriptPath -Encoding ASCII

        # Run diskpart
        $result = diskpart /s $scriptPath 2>&1
        Remove-Item $scriptPath -ErrorAction SilentlyContinue

        $sizeAfter = (Get-Item $Path).Length / 1GB
        $saved = $sizeBefore - $sizeAfter

        Write-Host "  Before: $([math]::Round($sizeBefore, 2)) GB" -ForegroundColor Gray
        Write-Host "  After:  $([math]::Round($sizeAfter, 2)) GB" -ForegroundColor Gray
        Write-Host "  Saved:  $([math]::Round($saved, 2)) GB" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host "Skipping $Name - file not found" -ForegroundColor Yellow
    }
}

# Compact both disks
Compact-VhdxFile -Path $dockerVhdx -Name "Docker"
Compact-VhdxFile -Path $ubuntuVhdx -Name "Ubuntu"

# Final status
Write-Host "=== Compaction Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Final disk sizes:" -ForegroundColor Yellow
if (Test-Path $dockerVhdx) {
    $dockerSize = (Get-Item $dockerVhdx).Length / 1GB
    Write-Host "  Docker VHDX: $([math]::Round($dockerSize, 2)) GB"
}
if (Test-Path $ubuntuVhdx) {
    $ubuntuSize = (Get-Item $ubuntuVhdx).Length / 1GB
    Write-Host "  Ubuntu VHDX: $([math]::Round($ubuntuSize, 2)) GB"
}
Write-Host ""
Write-Host "WSL will restart automatically when you open a terminal." -ForegroundColor Green
Write-Host "You can now close this window."
