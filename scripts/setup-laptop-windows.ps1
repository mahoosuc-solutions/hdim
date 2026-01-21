# setup-laptop-windows.ps1
#
# Windows PowerShell script to setup Docker Desktop for portable external drive
# Run this in PowerShell (Admin) on a new laptop before mounting the external drive
#
# Usage: .\scripts\setup-laptop-windows.ps1 -DriveLetter D -ExternalDrivePath "X:\DevEnvironment"
#
# Parameters:
#   -DriveLetter      : Letter of external drive (e.g., D, E, X)
#   -ExternalDrivePath: Path where external drive content is located
#   -StopDocker       : Stop Docker Desktop before configuration
#   -RestartDocker    : Restart Docker Desktop after configuration

param(
    [string]$DriveLetter = "X",
    [string]$ExternalDrivePath = "$($DriveLetter):\DevEnvironment",
    [switch]$StopDocker = $true,
    [switch]$RestartDocker = $true
)

# Colors and formatting
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Warning { Write-Host $args -ForegroundColor Yellow }
function Write-Error { Write-Host $args -ForegroundColor Red }
function Write-Info { Write-Host $args -ForegroundColor Cyan }

Write-Info "================================"
Write-Info "Docker Desktop Setup (Windows)"
Write-Info "================================"
Write-Info ""

# Check if running as admin
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error "ERROR: This script must be run as Administrator"
    Write-Info "Right-click PowerShell and select 'Run as Administrator'"
    exit 1
}

Write-Info "Administrator privileges confirmed"
Write-Info ""

# Check if external drive path exists
Write-Info "Checking external drive..."
if (-not (Test-Path $ExternalDrivePath)) {
    Write-Error "ERROR: External drive path not found: $ExternalDrivePath"
    Write-Info "Please connect the external WD-Black drive and mount it"
    Write-Info "Then specify the correct drive letter with -DriveLetter parameter"
    exit 1
}
Write-Success "✓ External drive found: $ExternalDrivePath"
Write-Info ""

# Check Docker Desktop installation
Write-Info "Checking Docker Desktop..."
$dockerPath = "$env:APPDATA\Docker"
if (-not (Test-Path $dockerPath)) {
    Write-Error "ERROR: Docker Desktop not found"
    Write-Info "Please install Docker Desktop first: https://www.docker.com/products/docker-desktop"
    exit 1
}
Write-Success "✓ Docker Desktop is installed"
Write-Info ""

# Stop Docker if requested
if ($StopDocker) {
    Write-Info "Stopping Docker Desktop..."
    Stop-Process -Name "Docker Desktop" -Force -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Seconds 2
    Write-Success "✓ Docker Desktop stopped"
} else {
    Write-Warning "⚠ WARNING: Docker Desktop should be stopped before reconfiguring"
    Write-Info "Stopping it now..."
    Stop-Process -Name "Docker Desktop" -Force -ErrorAction SilentlyContinue | Out-Null
    Start-Sleep -Seconds 2
}
Write-Info ""

# Backup existing daemon.json
Write-Info "Backing up Docker configuration..."
$daemonJsonPath = "$env:USERPROFILE\.docker\daemon.json"
if (Test-Path $daemonJsonPath) {
    $backupPath = "$daemonJsonPath.backup.$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Copy-Item $daemonJsonPath $backupPath
    Write-Success "✓ Backed up to: $backupPath"
} else {
    Write-Info "No existing daemon.json found"
}
Write-Info ""

# Create/update daemon.json with external drive configuration
Write-Info "Configuring Docker daemon.json..."

# Read template from external drive if available
$templatePath = "$ExternalDrivePath\config\daemon.json"
$daemonConfig = @{}

if (Test-Path $templatePath) {
    Write-Info "Using template from external drive: $templatePath"
    try {
        $daemonConfig = Get-Content $templatePath | ConvertFrom-Json
        Write-Success "✓ Loaded configuration template"
    } catch {
        Write-Warning "⚠ Could not parse template, using defaults"
    }
} else {
    Write-Info "Creating default configuration..."
}

# Ensure data-root is set to external drive path with Windows format
if (-not $daemonConfig.ContainsKey('data-root')) {
    $daemonConfig['data-root'] = "$ExternalDrivePath\docker-data"
}

# Add other important settings
$daemonConfig['builder'] = @{
    'gc' = @{
        'defaultKeepStorage' = '50GB'
        'enabled' = $true
    }
}
$daemonConfig['max-concurrent-downloads'] = 3
$daemonConfig['max-concurrent-uploads'] = 3
$daemonConfig['log-driver'] = 'json-file'
$daemonConfig['log-opts'] = @{
    'max-size' = '10m'
    'max-file' = 3
}

# Create .docker directory if it doesn't exist
$dockerConfigDir = "$env:USERPROFILE\.docker"
if (-not (Test-Path $dockerConfigDir)) {
    New-Item -ItemType Directory -Path $dockerConfigDir -Force | Out-Null
    Write-Info "Created .docker directory"
}

# Write daemon.json
$daemonJson = ConvertTo-Json $daemonConfig -Depth 10
$daemonJson | Out-File $daemonJsonPath -Encoding UTF8
Write-Success "✓ Updated: $daemonJsonPath"
Write-Info ""
Write-Info "Configuration:"
$daemonJson | ForEach-Object { Write-Info "  $_" }
Write-Info ""

# Restart Docker if requested
if ($RestartDocker) {
    Write-Info "Restarting Docker Desktop..."
    # Start Docker Desktop
    & "$env:APPDATA\Docker\Docker Desktop.exe" 2>$null &

    Write-Info "Waiting for Docker to start (this may take 30-60 seconds)..."
    $maxWait = 60
    $waited = 0
    while ($waited -lt $maxWait) {
        if ((Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue).Count -gt 0) {
            Write-Success "✓ Docker Desktop is starting"
            break
        }
        Start-Sleep -Seconds 1
        $waited++
    }

    Write-Info "Waiting for Docker daemon to be ready..."
    $maxWait = 120
    $waited = 0
    while ($waited -lt $maxWait) {
        $dockerReady = docker info 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "✓ Docker daemon is ready"
            break
        }
        Start-Sleep -Seconds 1
        $waited++
    }
} else {
    Write-Warning "⚠ Docker Desktop not restarted"
    Write-Info "Start Docker Desktop manually to apply changes"
}
Write-Info ""

# Verify configuration
Write-Info "Verifying Docker configuration..."
$dockerInfo = docker info 2>&1
if ($LASTEXITCODE -eq 0) {
    $dataRoot = $dockerInfo | Select-String "Docker Root Dir:" | ForEach-Object { $_ -replace ".*: ", "" }
    Write-Success "✓ Docker is running"
    Write-Info "  Data root: $dataRoot"
} else {
    Write-Warning "⚠ Docker is not yet running"
    Write-Info "  It may still be starting up"
}
Write-Info ""

# Create necessary directories on external drive
Write-Info "Initializing external drive directories..."
$dirsToCreate = @(
    "$ExternalDrivePath\docker-data",
    "$ExternalDrivePath\docker-data\images",
    "$ExternalDrivePath\docker-data\volumes",
    "$ExternalDrivePath\hdim-volumes\postgres",
    "$ExternalDrivePath\hdim-volumes\redis",
    "$ExternalDrivePath\hdim-volumes\kafka"
)

foreach ($dir in $dirsToCreate) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Success "✓ Created: $dir"
    } else {
        Write-Info "✓ Already exists: $dir"
    }
}
Write-Info ""

# Create docker-compose.override.yml template
Write-Info "Checking docker-compose configuration..."
$wslPath = "\\wsl$\Ubuntu-22.04"
$projectPath = "$wslPath\home\webemo-aaron\projects\hdim-master"

if (Test-Path $projectPath) {
    $overridePath = "$projectPath\docker-compose.override.yml"
    if (-not (Test-Path $overridePath)) {
        Write-Info "Copying docker-compose override template..."
        $templateSource = "$ExternalDrivePath\config\docker-compose.override.yml"
        if (Test-Path $templateSource) {
            Copy-Item $templateSource $overridePath -Force
            Write-Success "✓ Copied override template"
        } else {
            Write-Warning "⚠ Override template not found on external drive"
        }
    } else {
        Write-Info "Override template already exists"
    }
} else {
    Write-Warning "⚠ WSL/HDIM project path not accessible"
    Write-Info "This is optional - you can copy override template manually later"
}
Write-Info ""

# Final summary
Write-Info "================================"
Write-Success "✓ Setup Complete!"
Write-Info "================================"
Write-Info ""
Write-Info "Summary:"
Write-Info "1. Docker is configured to use: $ExternalDrivePath\docker-data"
Write-Info "2. External drive initialization complete"
Write-Info "3. docker-compose override configured (if available)"
Write-Info ""
Write-Info "Next Steps (in WSL):"
Write-Info "1. Mount the drive in WSL (if not already mounted):"
Write-Info "   sudo mount /dev/sdd /mnt/wd-black"
Write-Info ""
Write-Info "2. Verify Docker is working:"
Write-Info "   docker ps"
Write-Info ""
Write-Info "3. Test build (from HDIM project):"
Write-Info "   ./scripts/pre-build-check.sh"
Write-Info ""
Write-Info "Configuration file: $daemonJsonPath"
Write-Info ""
