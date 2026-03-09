# Google Cloud Run Deployment with Neon

## Prerequisites

1. Google Cloud SDK (gcloud) installed and authenticated
2. Docker installed
3. A Google Cloud project with billing enabled
4. A Neon PostgreSQL database

## Quick Start

### 1. Set up GCP Project

```bash
# Set your project
gcloud config set project remitos-backend

# Enable required APIs
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable secretmanager.googleapis.com
```

### 2. Push Image to Artifact Registry

```bash
./scripts/push-image.sh [PROJECT_ID] [REGION] [REPOSITORY] [IMAGE_NAME]
```

Example:
```bash
./scripts/push-image.sh remitos-backend southamerica-east1 remitos server
```

### 3. Store Secrets in Secret Manager

Store sensitive values in Google Secret Manager:

```bash
# Store database password (from your Neon connection string)
echo -n "your-neon-password" | gcloud secrets create db-password --data-file=-

# Store JWT secret
echo -n "your-jwt-secret-key" | gcloud secrets create jwt-secret --data-file=-

# Grant Cloud Run access to secrets
gcloud projects add-iam-policy-binding remitos-backend \
  --member="serviceAccount:$(gcloud projects describe remitos-backend --format='value(projectNumber)')-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

### 4. Grant Cloud Vision & Cloud Storage Permissions

The app uses **Application Default Credentials (ADC)** - no JSON key files needed! The Cloud Run service account automatically gets credentials.

Your service account: `backend-service@remitos-backend.iam.gserviceaccount.com`

Grant the required IAM roles:

```bash
# Your custom service account
SERVICE_ACCOUNT="backend-service@remitos-backend.iam.gserviceaccount.com"

# Grant Cloud Vision API user role
gcloud projects add-iam-policy-binding remitos-backend \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/vision.user"

# Grant Cloud Storage access (if using GCS)
gcloud projects add-iam-policy-binding remitos-backend \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/storage.objectAdmin"

# Grant access to Secret Manager
gcloud projects add-iam-policy-binding remitos-backend \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/secretmanager.secretAccessor"
```

### 5. Deploy to Cloud Run

Get your Neon connection details from the Neon dashboard, then deploy:

```bash
# Set environment variables
export DB_HOST="your-neon-host.aws.neon.tech"  # From Neon dashboard
export DB_USER="your-neon-user"                 # From Neon dashboard
export DB_NAME="your-neon-db"                   # From Neon dashboard
export DB_SSLMODE="require"                     # Required for Neon

# Deploy to remitos-backend project (uses backend-service@remitos-backend.iam.gserviceaccount.com by default)
./scripts/deploy-cloud-run.sh remitos-backend southamerica-east1 remitos server remitos-api

# Deploy with a different service account (6th parameter)
./scripts/deploy-cloud-run.sh remitos-backend southamerica-east1 remitos server remitos-api backend-service@remitos-backend.iam.gserviceaccount.com
```

Or deploy with inline environment variables:

```bash
# Using your custom service account (backend-service@remitos-backend.iam.gserviceaccount.com)
gcloud run deploy remitos-api \
  --image southamerica-east1-docker.pkg.dev/remitos-backend/remitos/server:latest \
  --region southamerica-east1 \
  --platform managed \
  --allow-unauthenticated \
  --service-account backend-service@remitos-backend.iam.gserviceaccount.com \
  --set-env-vars "DB_HOST=your-neon-host.aws.neon.tech,DB_PORT=5432,DB_USER=your-user,DB_NAME=your-db,DB_SSLMODE=require" \
  --update-secrets DB_PASSWORD=db-password:latest,JWT_SECRET=jwt-secret:latest \
  --project remitos-backend

# Using default Compute Engine service account (not recommended)
gcloud run deploy remitos-api \
  --image southamerica-east1-docker.pkg.dev/remitos-backend/remitos/server:latest \
  --region southamerica-east1 \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars "DB_HOST=your-neon-host.aws.neon.tech,DB_PORT=5432,DB_USER=your-user,DB_NAME=your-db,DB_SSLMODE=require" \
  --update-secrets DB_PASSWORD=db-password:latest,JWT_SECRET=jwt-secret:latest \
  --project remitos-backend
```

## Application Default Credentials (ADC)

This application uses **Application Default Credentials** for authenticating with Google Cloud services:

- **Cloud Vision API** - For OCR/text detection
- **Cloud Storage** - For file storage (if implemented)

### How it works:

1. **Local Development**: ADC looks for credentials in this order:
   - `GOOGLE_APPLICATION_CREDENTIALS` environment variable (path to JSON key)
   - User credentials from `gcloud auth application-default login`
   - GCE/App Engine metadata server

2. **Cloud Run**: Automatically uses the service account attached to the service

### Local Development Setup

```bash
# Option 1: Use gcloud credentials (recommended)
gcloud auth application-default login

# Option 2: Set path to service account JSON (if needed)
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key.json
```

### Production (Cloud Run)

No configuration needed! Just ensure the service account has the right IAM roles (see step 4 above).

## Neon Configuration

### Connection String Format

Your Neon connection string will look like:
```
postgres://user:password@host.aws.neon.tech/dbname?sslmode=require
```

Break this down into environment variables:
- `DB_HOST`: `host.aws.neon.tech`
- `DB_PORT`: `5432`
- `DB_USER`: `user`
- `DB_PASSWORD`: `password` (use Secret Manager)
- `DB_NAME`: `dbname`

### Network Security

For production, consider:
1. **Neon's IP Allow List**: Restrict connections to Cloud Run's egress IPs
2. **SSL Mode**: Neon requires SSL (`sslmode=require`)
3. **Branch Strategy**: Use separate Neon branches for dev/staging/prod

## Environment Variables

| Variable | Description | Source |
|----------|-------------|--------|
| `DB_HOST` | Neon hostname | Neon Dashboard |
| `DB_PORT` | Database port | `5432` |
| `DB_USER` | Neon username | Neon Dashboard |
| `DB_PASSWORD` | Neon password | Secret Manager |
| `DB_NAME` | Database name | Neon Dashboard |
| `DB_SSLMODE` | SSL mode (must be `require` for Neon) | `require` |
| `JWT_SECRET` | JWT signing secret | Secret Manager |

## Useful Commands

```bash
# View logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=remitos-api" --limit=50

# View service details
gcloud run services describe remitos-api --region southamerica-east1

# Update environment variables
gcloud run services update remitos-api \
  --region southamerica-east1 \
  --set-env-vars "KEY=value"

# Update secrets
gcloud run services update remitos-api \
  --region southamerica-east1 \
  --update-secrets DB_PASSWORD=db-password:latest,JWT_SECRET=jwt-secret:latest

# Redeploy with new image
gcloud run deploy remitos-api \
  --image southamerica-east1-docker.pkg.dev/remitos-backend/remitos/server:latest \
  --region southamerica-east1 \
  --project remitos-backend
```

## Troubleshooting

### Connection Issues

If you see connection errors:
1. Verify the Neon hostname is correct
2. Check that `sslmode=require` is being used (Neon requires SSL)
3. Ensure the password in Secret Manager matches your Neon credentials
4. Verify Cloud Run has network egress access (default allows all outbound)

### Migration Failures

The app runs migrations on startup. If migrations fail:
1. Check database user has CREATE TABLE permissions
2. Verify `db/migrations` directory is included in the Docker image
3. Check logs for specific migration errors

### Vision API Permission Denied

If you see errors like `PermissionDenied` when calling Cloud Vision:

```bash
# Verify your service account has the vision.user role
SERVICE_ACCOUNT="backend-service@remitos-backend.iam.gserviceaccount.com"

gcloud projects get-iam-policy remitos-backend \
  --flatten="bindings[].members" \
  --format="table(bindings.role)" \
  --filter="bindings.members:$SERVICE_ACCOUNT"

# If missing, add it
gcloud projects add-iam-policy-binding remitos-backend \
  --member="serviceAccount:$SERVICE_ACCOUNT" \
  --role="roles/vision.user"
```

Also verify the Vision API is enabled:
```bash
gcloud services enable vision.googleapis.com
```
