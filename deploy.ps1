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
$REGION = "us-east1" # You can change this to your preferred region
$IMAGE_TAG = "gcr.io/$currentProject/$SERVICE_NAME"
$BUILD_BUCKET = "gs://$currentProject-builds/source"

Write-Host "--- Configuration ---" -ForegroundColor Cyan
Write-Host "Project ID:   $currentProject"
Write-Host "Service Name: $SERVICE_NAME"
Write-Host "Region:       $REGION"
Write-Host "Image Tag:    $IMAGE_TAG"
Write-Host "Build Bucket: $BUILD_BUCKET"
Write-Host "---------------------"
Write-Host ""

# --- Step 1: Build & Push Container (Cloud Build) ---
Write-Host "1. Building and Pushing Container Image (Cloud Build)..." -ForegroundColor Green
& gcloud builds submit --tag $IMAGE_TAG --gcs-source-staging-dir $BUILD_BUCKET .

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# --- Step 2: Ensure Storage Bucket Exists ---
$BUCKET_NAME = "$currentProject-poker-data"
Write-Host "2. Checking for storage bucket: $BUCKET_NAME..." -ForegroundColor Green

# Check if bucket exists
$bucketExists = gcloud storage buckets list --filter="name:$BUCKET_NAME" --format="value(name)"
if (-not $bucketExists) {
    Write-Host "Creating bucket $BUCKET_NAME..." -ForegroundColor Cyan
    & gcloud storage buckets create gs://$BUCKET_NAME --location=$REGION
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to create bucket!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
} else {
    Write-Host "Bucket already exists." -ForegroundColor Green
}
Write-Host ""

# --- Step 3: Deploy to Cloud Run ---
Write-Host "3. Deploying to Cloud Run..." -ForegroundColor Green
& gcloud beta run deploy $SERVICE_NAME `
    --image $IMAGE_TAG `
    --platform managed `
    --region $REGION `
    --allow-unauthenticated `
    --port 8080 `
    --execution-environment gen2 `
    --add-volume "name=poker-storage,type=cloud-storage,bucket=$BUCKET_NAME" `
    --add-volume-mount "volume=poker-storage,mount-path=/app/data/rooms"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Deployment failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "--- Deployment Complete! ---" -ForegroundColor Green
Write-Host "Your app should now be live."
