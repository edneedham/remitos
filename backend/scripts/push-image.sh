#!/bin/bash
set -e

PROJECT_ID=${1:-remitos-backend}
REGION=${2:-southamerica-east1}
REPOSITORY=${3:-remitos}
IMAGE_NAME=${4:-server}
SERVICE_NAME=${5:-remitos-api}

echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Repository: $REPOSITORY"
echo "Image: $IMAGE_NAME"
echo "Service: $SERVICE_NAME"

IMAGE_URL="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/${IMAGE_NAME}"

echo "Setting up Artifact Registry..."
gcloud artifacts repositories create ${REPOSITORY} \
  --repository-format=docker \
  --location=${REGION} \
  --description="Remitos application repository" 2>/dev/null || echo "Repository already exists"

echo "Authenticating with Artifact Registry..."
gcloud auth configure-docker ${REGION}-docker.pkg.dev

echo "Building Docker image for linux/amd64..."
docker buildx build --platform linux/amd64 -t ${IMAGE_URL}:latest . --load

echo "Pushing image to Artifact Registry..."
docker push ${IMAGE_URL}:latest

echo ""
echo "Image pushed to: ${IMAGE_URL}:latest"
echo ""
echo "To deploy to Cloud Run, run:"
echo "  ./scripts/deploy-cloud-run.sh ${PROJECT_ID} ${REGION} ${REPOSITORY} ${IMAGE_NAME} ${SERVICE_NAME}"
