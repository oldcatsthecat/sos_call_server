#!/bin/bash
set -e

PROJECT_NAME="sos-call-server"
IMAGE_TAG="${1:-latest}"

echo "========================================="
echo "  SOS Emergency Call Server Deploy"
echo "========================================="

case "${2:-up}" in
    down)
        echo "[1/1] Stopping and removing containers..."
        docker compose down
        echo "Done. Server stopped."
        exit 0
        ;;
    restart)
        echo "[1/2] Stopping..."
        docker compose down
        echo "[2/2] Starting..."
        docker compose up -d --build
        echo "Done."
        exit 0
        ;;
    logs)
        docker compose logs -f --tail=100
        exit 0
        ;;
esac

echo "[1/2] Building image..."
docker compose build

echo "[2/2] Starting container..."
docker compose up -d

echo ""
echo "========================================="
echo "  Server is running!"
echo "  Check:  http://localhost:8080/api/health"
echo "  Logs:   docker compose logs -f"
echo "  Stop:   ./deploy.sh latest down"
echo "========================================="
