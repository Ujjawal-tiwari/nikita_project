$body = @{ customerId = "CUST001" } | ConvertTo-Json
Write-Host "Calling /api/risk/langchain..."
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/risk/langchain" -Method Post -ContentType "application/json" -Body $body
$response | ConvertTo-Json -Depth 5
