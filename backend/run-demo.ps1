# Levanta MySQL (Docker) y el backend con los datos demo (90 días de historial y agenda hasta el 31 de diciembre).
Set-Location $PSScriptRoot
docker compose up -d
if ($LASTEXITCODE -ne 0) { docker start recovr-mysql }
Write-Host "Esperando a MySQL..."
while ((docker inspect -f '{{.State.Health.Status}}' recovr-mysql 2>$null) -ne 'healthy') { Start-Sleep -Seconds 2 }
$env:SPRING_PROFILES_ACTIVE = "demo"
mvn spring-boot:run
