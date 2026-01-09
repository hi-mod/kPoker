# deploy.ps1
# Automates deployment of the Poker application to Google Cloud Run

$ErrorActionPreference = "Stop"

# --- Configuration ---
# Try to get project ID from gcloud config if not provided
$currentProject = gcloud config get-value project 2> $null
if (-not $currentProject) {
    Write-Host "Error: No Google Cloud Project ID set." -ForegroundColor Red
    Write-Host "Please set it using: gcloud config set project YOUR_PROJECT_ID"
    exit 1
}

$SERVICE_NAME = "poker-server"
$REGION = "us-central1" # You can change this to your preferred region
$IMAGE_TAG = "gcr.io/$currentProject/$SERVICE_NAME"

Write-Host "--- Configuration ---" -ForegroundColor Cyan
Write-Host "Project ID:   $currentProject"
Write-Host "Service Name: $SERVICE_NAME"
Write-Host "Region:       $REGION"
Write-Host "Image Tag:    $IMAGE_TAG"
Write-Host "---------------------"
Write-Host ""

# --- Step 1: Build Wasm client locally ---
Write-Host "1. Building Wasm client locally..." -ForegroundColor Green
& .\gradlew.bat :composeApp:wasmJsBrowserDistribution --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host "Wasm build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Wasm client built successfully." -ForegroundColor Green
Write-Host ""

# --- Step 2: Copy Wasm dist to server resources ---
Write-Host "2. Copying Wasm files to server resources..." -ForegroundColor Green
$wasmSource = "composeApp\build\dist\wasmJs\productionExecutable"
$wasmDest = "server\src\main\resources\static"

# Create static directory if it doesn't exist
if (-not (Test-Path $wasmDest)) {
    New-Item -ItemType Directory -Path $wasmDest -Force | Out-Null
}

# Clean and copy
Remove-Item -Path "$wasmDest\*" -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item -Path "$wasmSource\*" -Destination $wasmDest -Recurse -Force

Write-Host "Wasm files copied to $wasmDest" -ForegroundColor Green
Write-Host ""

# --- Step 3: Build & Push Container ---
Write-Host "3. Building and Pushing Container Image..." -ForegroundColor Green
& gcloud builds submit --tag $IMAGE_TAG .

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# --- Step 4: Deploy to Cloud Run ---
Write-Host "4. Deploying to Cloud Run..." -ForegroundColor Green
& gcloud run deploy $SERVICE_NAME `
    --image $IMAGE_TAG `
    --platform managed `
    --region $REGION `
    --allow-unauthenticated `
    --port 8080

if ($LASTEXITCODE -ne 0) {
    Write-Host "Deployment failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "--- Deployment Complete! ---" -ForegroundColor Green
Write-Host "Your app should now be live."
