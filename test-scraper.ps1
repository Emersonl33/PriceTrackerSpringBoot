# Script para testar o Price Tracker no Windows PowerShell
# Uso: .\test-scraper.ps1

$BASE_URL = "http://localhost:8081"
$EMAIL = "teste@$(Get-Date -Format 'yyyyMMddHHmmss').com"
$PASSWORD = "teste123456"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "🧪 TESTE DO PRICE TRACKER - SCRAPER FIXES" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

try {
    # 1. Registrar novo usuário
    Write-Host "1️⃣  Registrando novo usuário..." -ForegroundColor Yellow
    $registerBody = @{
        email = $EMAIL
        password = $PASSWORD
    } | ConvertTo-Json

    Invoke-RestMethod -Uri "$BASE_URL/auth/register" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $registerBody | Out-Null

    Write-Host "✓ Usuário registrado: $EMAIL" -ForegroundColor Green
    Write-Host ""

    # 2. Fazer login
    Write-Host "2️⃣  Fazendo login..." -ForegroundColor Yellow
    $loginBody = @{
        email = $EMAIL
        password = $PASSWORD
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/login" `
        -Method Post `
        -Headers @{"Content-Type" = "application/json"} `
        -Body $loginBody

    $TOKEN = $loginResponse.token
    if (-not $TOKEN) {
        Write-Host "✗ Erro: Falha no login" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Token obtido" -ForegroundColor Green
    Write-Host ""

    # 3. Adicionar primeiro produto
    Write-Host "3️⃣  Adicionando primeiro produto para rastreamento..." -ForegroundColor Yellow
    $product1Body = @{
        url = "https://www.mercadolivre.com.br/"
        name = "Produto Teste 1 - Mercado Livre"
    } | ConvertTo-Json

    $product1 = Invoke-RestMethod -Uri "$BASE_URL/products" `
        -Method Post `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $TOKEN"
        } `
        -Body $product1Body

    $PRODUCT1_ID = $product1.productId
    Write-Host "✓ Produto 1 adicionado (ID: $PRODUCT1_ID)" -ForegroundColor Green
    Write-Host ""

    # 4. Adicionar segundo produto
    Write-Host "4️⃣  Adicionando segundo produto..." -ForegroundColor Yellow
    $product2Body = @{
        url = "https://www.amazon.com.br/"
        name = "Produto Teste 2 - Amazon"
    } | ConvertTo-Json

    $product2 = Invoke-RestMethod -Uri "$BASE_URL/products" `
        -Method Post `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $TOKEN"
        } `
        -Body $product2Body

    $PRODUCT2_ID = $product2.productId
    Write-Host "✓ Produto 2 adicionado (ID: $PRODUCT2_ID)" -ForegroundColor Green
    Write-Host ""

    # 5. Listar produtos
    Write-Host "5️⃣  Listando produtos cadastrados..." -ForegroundColor Yellow
    $products = Invoke-RestMethod -Uri "$BASE_URL/products" `
        -Method Get `
        -Headers @{"Authorization" = "Bearer $TOKEN"}

    $PRODUCT_COUNT = $products.Count
    Write-Host "✓ Total de produtos: $PRODUCT_COUNT" -ForegroundColor Green
    $products | Format-Table -AutoSize
    Write-Host ""

    # 6. Aguardar scheduler
    Write-Host "6️⃣  Aguardando scheduler (30 segundos)..." -ForegroundColor Yellow
    Write-Host "ℹ️  O scheduler está configurado para rodar a cada 10 segundos" -ForegroundColor Cyan
    Write-Host "ℹ️  Aguardando 30 segundos..." -ForegroundColor Cyan

    for ($i = 30; $i -ge 1; $i--) {
        Write-Host -NoNewline "`r⏳ Aguardando... $($i)s    "
        Start-Sleep -Seconds 1
    }
    Write-Host "`r✓ Tempo decorrido                  " -ForegroundColor Green
    Write-Host ""

    # 7. Consultar histórico do primeiro produto
    Write-Host "7️⃣  Consultando histórico de preços (Produto 1)..." -ForegroundColor Yellow
    $history = Invoke-RestMethod -Uri "$BASE_URL/products/$PRODUCT1_ID/history" `
        -Method Get `
        -Headers @{"Authorization" = "Bearer $TOKEN"}

    $HISTORY_COUNT = if ($history -is [array]) { $history.Count } else { 1 }

    if ($HISTORY_COUNT -eq 0 -or $null -eq $history) {
        Write-Host "✗ Nenhum snapshot encontrado no histórico!" -ForegroundColor Red
        Write-Host "ℹ️  Possíveis motivos:" -ForegroundColor Yellow
        Write-Host "   - O scheduler ainda não foi executado" -ForegroundColor Yellow
        Write-Host "   - Houve erro ao fazer scraping da URL" -ForegroundColor Yellow
        Write-Host "   - O DynamoDB não está funcionando" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Verifique os logs da aplicação para mais detalhes:" -ForegroundColor Yellow
    } else {
        Write-Host "✓ Histórico encontrado! Total de snapshots: $HISTORY_COUNT" -ForegroundColor Green
        $history | Format-Table -AutoSize
    }
    Write-Host ""

    # 8. Resumo
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "✓ TESTE CONCLUÍDO" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Resumo:" -ForegroundColor White
    Write-Host "  • Email: $EMAIL"
    Write-Host "  • Produto 1 ID: $PRODUCT1_ID"
    Write-Host "  • Produto 2 ID: $PRODUCT2_ID"
    Write-Host "  • Snapshots encontrados: $HISTORY_COUNT"
    Write-Host ""
    Write-Host "Próximos passos:" -ForegroundColor Cyan
    Write-Host "  1. Aguarde mais alguns ciclos do scheduler (múltiplos de 10s)" -ForegroundColor Cyan
    Write-Host "  2. Execute este script novamente para ver a evolução" -ForegroundColor Cyan
    Write-Host "  3. Verifique os logs da aplicação" -ForegroundColor Cyan
    Write-Host ""

} catch {
    Write-Host "❌ ERRO: $_" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

