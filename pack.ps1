# 一键打包：后端 JAR + 前端 dist（用于上传阿里云 ECS 等）
# 在项目根目录执行: powershell -ExecutionPolicy Bypass -File pack.ps1

Set-Location $PSScriptRoot

Write-Host "Building backend..."
Set-Location retail-backend
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { exit 1 }
Set-Location ..

Write-Host "Building frontend..."
Set-Location retail-frontend
if (Test-Path package-lock.json) { npm ci } else { npm install }
npm run build
if ($LASTEXITCODE -ne 0) { exit 1 }
Set-Location ..

Write-Host "Done."
Write-Host "  Backend:  retail-backend\target\retail-backend-1.0.0.jar"
Write-Host "  Frontend: retail-frontend\dist\"
