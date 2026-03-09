#!/bin/bash
set -e

PROJECT_ID=${1:-remitos-backend}
REGION=${2:-southamerica-east1}
REPOSITORY=${3:-remitos}
IMAGE_NAME=${4:-server}
SERVICE_NAME=${5:-remitos-api}
SERVICE_ACCOUNT=${6:-backend-service@remitos-backend.iam.gserviceaccount.com}

echo "Project: $PROJECT_ID"
echo "Region: $REGION"
echo "Repository: $REPOSITORY"
echo "Image: $IMAGE_NAME"
echo "Service: $SERVICE_NAME"
if [ -n "$SERVICE_ACCOUNT" ]; then
  echo "Service Account: $SERVICE_ACCOUNT"
fi

IMAGE_URL="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY}/${IMAGE_NAME}"

echo ""
echo "Note: Using Neon PostgreSQL"
echo "Make sure you have set these environment variables:"
echo "  - DB_HOST (Neon hostname, e.g., xxx.aws.neon.tech)"
echo "  - DB_PORT (default: 5432)"
echo "  - DB_USER (Neon username)"
echo "  - DB_NAME (Neon database name)"
echo "  - DB_SSLMODE (must be 'require' for Neon, default: require)"
echo "  - DB_PASSWORD (stored in Secret Manager: db-password)"
echo "  - JWT_SECRET (stored in Secret Manager: jwt-secret)"
echo ""

echo "Deploying to Cloud Run..."

# Build command arguments in an array
deploy_args=(
  "run" "deploy" "${SERVICE_NAME}"
  "--image" "${IMAGE_URL}:latest"
  "--region" "${REGION}"
  "--platform" "managed"
  "--allow-unauthenticated"
  "--set-env-vars" "DB_HOST=${DB_HOST:-},DB_PORT=${DB_PORT:-5432},DB_USER=${DB_USER:-},DB_NAME=${DB_NAME:-},DB_SSLMODE=${DB_SSLMODE:-require}"
  "--update-secrets" "DB_PASSWORD=db-password:latest,JWT_SECRET=jwt-secret:latest"
  "--project" "${PROJECT_ID}"
)

# Add service account if provided
if [ -n "$SERVICE_ACCOUNT" ]; then
  deploy_args+=("--service-account" "${SERVICE_ACCOUNT}")
fi

gcloud "${deploy_args[@]}"

echo ""
echo "Deployment complete!"
echo "Service URL: $(gcloud run services describe ${SERVICE_NAME} --region ${REGION} --format 'value(status.url)')"

echo ""
echo "To update the service account later, run:"
echo "  gcloud run services update ${SERVICE_NAME} --region ${REGION} --service-account YOUR_SERVICE_ACCOUNT_EMAIL"
