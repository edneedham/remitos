#!/usr/bin/env bash

# Seed Google Cloud SQL database with demo data
# Usage: ./scripts/seed-cloud-db.sh [INSTANCE_NAME] [DATABASE_NAME] [USER]

set -euo pipefail

# Default values
INSTANCE_NAME="${1:-remitos-db}"
DATABASE_NAME="${2:-remitos}"
DB_USER="${3:-postgres}"
SEED_FILE="backend/db/seed_demo.sql"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    log_error "gcloud CLI is not installed. Please install it first:"
    log_error "  https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if we're authenticated
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q '@'; then
    log_error "Not authenticated with gcloud. Please run:"
    log_error "  gcloud auth login"
    exit 1
fi

# Check if seed file exists
if [[ ! -f "$SEED_FILE" ]]; then
    log_error "Seed file not found: $SEED_FILE"
    log_error "Please run this script from the project root directory."
    exit 1
fi

log_info "Seeding Cloud SQL database..."
log_info "  Instance: $INSTANCE_NAME"
log_info "  Database: $DATABASE_NAME"
log_info "  User: $DB_USER"
log_info "  Seed file: $SEED_FILE"
echo

# Method 1: Try using gcloud sql connect with pipe
log_info "Attempting to connect via gcloud sql connect..."

if gcloud sql connect "$INSTANCE_NAME" --user="$DB_USER" --database="$DATABASE_NAME" --quiet < "$SEED_FILE" 2>/tmp/seed_error.log; then
    log_info "✓ Database seeded successfully!"
    log_info "Demo company 'Logística del Sur S.A.' (LOGSUR) created with:"
    log_info "  - Warehouse: Depósito Central"
    log_info "  - Users: admin, jefedeposito, operador"
    log_info "  - Password for all users: demo1234"
    exit 0
else
    log_warn "Direct connection failed. Trying alternative method..."
fi

# Method 2: Get connection name and use cloud_sql_proxy or direct psql
log_info "Getting Cloud SQL connection info..."

CONNECTION_NAME=$(gcloud sql instances describe "$INSTANCE_NAME" --format='value(connectionName)' 2>/dev/null || true)

if [[ -z "$CONNECTION_NAME" ]]; then
    log_error "Could not get connection name for instance: $INSTANCE_NAME"
    log_error "Available instances:"
    gcloud sql instances list --format="table(name, region, connectionName)" 2>/dev/null || true
    exit 1
fi

log_info "Connection name: $CONNECTION_NAME"

# Check if cloud_sql_proxy is available
if command -v cloud_sql_proxy &> /dev/null; then
    log_info "Using cloud_sql_proxy..."
    
    # Start proxy in background
    PROXY_PORT=5433
    cloud_sql_proxy -instances="$CONNECTION_NAME=tcp:$PROXY_PORT" -credential_file="" &
    PROXY_PID=$!
    
    # Wait for proxy to start
    sleep 3
    
    # Run seed SQL
    if PGPASSWORD="$(gcloud sql users list --instance="$INSTANCE_NAME" --format="value(password)" 2>/dev/null || echo '')" \
       psql -h localhost -p $PROXY_PORT -U "$DB_USER" -d "$DATABASE_NAME" -f "$SEED_FILE" 2>/tmp/seed_error.log; then
        log_info "✓ Database seeded successfully via proxy!"
        kill $PROXY_PID 2>/dev/null || true
        exit 0
    else
        log_warn "Proxy connection failed."
        kill $PROXY_PID 2>/dev/null || true
    fi
fi

# Method 3: Try Cloud Shell approach with gcloud
log_info "Attempting connection via Cloud Shell..."
log_warn "This will open an interactive shell. Please run this command inside:"
echo
echo -e "${YELLOW}\\$ psql -h /cloudsql/$CONNECTION_NAME -U $DB_USER -d $DATABASE_NAME -f /tmp/seed_demo.sql${NC}"
echo

# Copy seed file to a temporary location that can be accessed
cat << 'INSTRUCTIONS'

Alternative manual methods:

1. Using Cloud Console Query Editor:
   - Go to: https://console.cloud.google.com/sql/instances
   - Click on your instance: INSTANCE_NAME
   - Go to "Databases" tab
   - Click "Open Query Editor" or "Connect using Cloud Shell"
   - Copy and paste the SQL from: backend/db/seed_demo.sql

2. Using psql with IAM authentication:
   gcloud sql connect INSTANCE_NAME --user=postgres --database=remitos
   Then paste the SQL from backend/db/seed_demo.sql

3. Using pgAdmin or DBeaver:
   - Connection name: CONNECTION_NAME
   - User: postgres
   - Database: remitos
   - Run the seed_demo.sql file

INSTRUCTIONS

# Show the SQL that needs to be run
log_info "SQL to execute (from $SEED_FILE):"
echo "---"
head -20 "$SEED_FILE"
echo "..."
echo "---"

log_error "Automatic seeding failed. Please use one of the manual methods above."
exit 1
