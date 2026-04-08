#!/usr/bin/env bash
#
# Seed production database with demo data
# Usage: ./seed-production.sh
#

set -e

# Production API endpoint
API_URL="https://remitos-api-865349418409.southamerica-east1.run.app"

echo "Seeding production database with demo data..."
echo "API URL: $API_URL"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to make API calls
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -n "$data" ]; then
        curl -s -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "${API_URL}${endpoint}"
    else
        curl -s -X "$method" \
            -H "Content-Type: application/json" \
            "${API_URL}${endpoint}"
    fi
}

# Step 1: Check if API is healthy
echo -e "${YELLOW}Step 1: Checking API health...${NC}"
HEALTH_RESPONSE=$(make_request "GET" "/health")
if echo "$HEALTH_RESPONSE" | grep -q "healthy\|ok\|OK"; then
    echo -e "${GREEN}✓ API is healthy${NC}"
else
    echo -e "${RED}✗ API health check failed${NC}"
    echo "Response: $HEALTH_RESPONSE"
    exit 1
fi
echo ""

# Step 2: Create LOGSUR company with admin user
echo -e "${YELLOW}Step 2: Creating LOGSUR company and admin user...${NC}"
ADMIN_RESPONSE=$(make_request "POST" "/auth/register" '{
    "username": "admin",
    "password": "demo1234",
    "role": "company_owner",
    "company_code": "LOGSUR",
    "company_name": "Logística del Sur S.A."
}')

if echo "$ADMIN_RESPONSE" | grep -q "error\|Error"; then
    echo -e "${RED}✗ Failed to create admin user${NC}"
    echo "Response: $ADMIN_RESPONSE"
    # Continue anyway - user might already exist
    echo -e "${YELLOW}Continuing (user might already exist)...${NC}"
else
    echo -e "${GREEN}✓ Admin user created${NC}"
    echo "Response: $ADMIN_RESPONSE"
fi
echo ""

# Step 3: Login as admin to get token
echo -e "${YELLOW}Step 3: Logging in as admin...${NC}"
LOGIN_RESPONSE=$(make_request "POST" "/auth/login" '{
    "company_code": "LOGSUR",
    "username": "admin",
    "password": "demo1234"
}')

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    echo -e "${GREEN}✓ Admin login successful${NC}"
    # Extract token (basic extraction, might need jq for production)
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    echo "Token obtained"
else
    echo -e "${RED}✗ Admin login failed${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi
echo ""

# Step 4: Create warehouse admin
echo -e "${YELLOW}Step 4: Creating warehouse admin...${NC}"
JEFE_RESPONSE=$(make_request "POST" "/auth/register" '{
    "username": "jefedeposito",
    "password": "demo1234",
    "role": "warehouse_admin",
    "company_code": "LOGSUR"
}')

if echo "$JEFE_RESPONSE" | grep -q "error\|Error"; then
    echo -e "${YELLOW}⚠ Warehouse admin might already exist${NC}"
else
    echo -e "${GREEN}✓ Warehouse admin created${NC}"
fi
echo ""

# Step 5: Create operators
echo -e "${YELLOW}Step 5: Creating operators...${NC}"

OPERATORS=(
    "m.gomez:Miguel Gomez"
    "j.perez:Juan Perez"
    "l.rodriguez:Lucia Rodriguez"
)

for op in "${OPERATORS[@]}"; do
    IFS=':' read -r username fullname <<< "$op"
    
    echo "Creating operator: $username"
    OP_RESPONSE=$(make_request "POST" "/auth/register" "{
        \"username\": \"$username\",
        \"password\": \"demo1234\",
        \"role\": \"operator\",
        \"company_code\": \"LOGSUR\"
    }")
    
    if echo "$OP_RESPONSE" | grep -q "error\|Error"; then
        echo -e "${YELLOW}  ⚠ $username might already exist${NC}"
    else
        echo -e "${GREEN}  ✓ $username created${NC}"
    fi
done
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Demo data seeding completed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Company: LOGSUR"
echo "Users:"
echo "  - admin (company_owner)"
echo "  - jefedeposito (warehouse_admin)"
echo "  - m.gomez (operator)"
echo "  - j.perez (operator)"
echo "  - l.rodriguez (operator - inactive)"
echo "Password for all: demo1234"
echo ""
echo "You can now log in with these credentials."
