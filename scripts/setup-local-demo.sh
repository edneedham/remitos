#!/bin/bash
# setup-local-demo.sh - Run this to set up local demo environment for screenshots

set -e

echo "🚀 Setting up local demo environment for screenshots..."

# Step 1: Start PostgreSQL in Docker
echo "📦 Starting PostgreSQL container..."
docker run --name remitos-demo-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=server \
  -p 5432:5432 \
  -d postgres:15-alpine

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to start..."
sleep 5

# Step 2: Start backend (in background)
echo "🔧 Starting backend server..."
cd backend
go run main.go &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 5

# Step 3: Seed the database
echo "🌱 Seeding database with demo data..."
cd ..
docker exec -i remitos-demo-db psql -U postgres -d server < backend/db/seed_demo.sql

echo ""
echo "✅ Demo environment ready!"
echo ""
echo "Next steps:"
echo "1. Open Android Studio"
echo "2. Clear app storage on emulator (Device Manager → Wipe Data)"
echo "3. Run the app and login with:"
echo "   - Código: LOGSUR"
echo "   - Usuario: admin"
echo "   - Contraseña: demo1234"
echo ""
echo "When done with screenshots:"
echo "  docker rm -f remitos-demo-db"
echo "  kill $BACKEND_PID"
