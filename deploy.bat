@echo off
setlocal

set ACTION=%1
if "%ACTION%"=="" set ACTION=up

echo =========================================
echo   SOS Emergency Call Server Deploy
echo =========================================

if "%ACTION%"=="down" (
    echo [1/1] Stopping and removing containers...
    docker compose down
    echo Done. Server stopped.
    exit /b 0
)

if "%ACTION%"=="restart" (
    echo [1/2] Stopping...
    docker compose down
    echo [2/2] Starting...
    docker compose up -d --build
    echo Done.
    exit /b 0
)

if "%ACTION%"=="logs" (
    docker compose logs -f --tail=100
    exit /b 0
)

echo [1/2] Building image...
docker compose build

echo [2/2] Starting container...
docker compose up -d

echo.
echo =========================================
echo   Server is running!
echo   Check:  http://localhost:8080/api/health
echo   Logs:   deploy.bat logs
echo   Stop:   deploy.bat down
echo =========================================
