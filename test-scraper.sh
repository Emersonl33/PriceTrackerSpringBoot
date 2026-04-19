#!/bin/bash
# Script para testar o Price Tracker com as correções

set -e

BASE_URL="http://localhost:8081"
EMAIL="teste@$(date +%s).com"
PASSWORD="teste123456"

echo "============================================"
echo "🧪 TESTE DO PRICE TRACKER - SCRAPER FIXES"
echo "============================================"
echo ""

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. Registrar novo usuário
echo -e "${YELLOW}1️⃣  Registrando novo usuário...${NC}"
curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" > /dev/null
echo -e "${GREEN}✓ Usuário registrado: $EMAIL${NC}\n"

# 2. Fazer login
echo -e "${YELLOW}2️⃣  Fazendo login...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token // empty')
if [ -z "$TOKEN" ]; then
  echo -e "${RED}✗ Erro: Falha no login${NC}"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi
echo -e "${GREEN}✓ Token obtido${NC}\n"

# 3. Adicionar primeiro produto
echo -e "${YELLOW}3️⃣  Adicionando primeiro produto para rastreamento...${NC}"
PRODUCT1=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "url":"https://www.mercadolivre.com.br/",
    "name":"Produto Teste 1 - Mercado Livre"
  }')

PRODUCT1_ID=$(echo $PRODUCT1 | jq -r '.productId // empty')
if [ -z "$PRODUCT1_ID" ]; then
  echo -e "${RED}✗ Erro ao adicionar produto${NC}"
  echo "Response: $PRODUCT1"
  exit 1
fi
echo -e "${GREEN}✓ Produto 1 adicionado (ID: $PRODUCT1_ID)${NC}\n"

# 4. Adicionar segundo produto
echo -e "${YELLOW}4️⃣  Adicionando segundo produto...${NC}"
PRODUCT2=$(curl -s -X POST $BASE_URL/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "url":"https://www.amazon.com.br/",
    "name":"Produto Teste 2 - Amazon"
  }')

PRODUCT2_ID=$(echo $PRODUCT2 | jq -r '.productId // empty')
echo -e "${GREEN}✓ Produto 2 adicionado (ID: $PRODUCT2_ID)${NC}\n"

# 5. Listar produtos
echo -e "${YELLOW}5️⃣  Listando produtos cadastrados...${NC}"
PRODUCTS=$(curl -s -X GET $BASE_URL/products \
  -H "Authorization: Bearer $TOKEN")
PRODUCT_COUNT=$(echo $PRODUCTS | jq 'length')
echo -e "${GREEN}✓ Total de produtos: $PRODUCT_COUNT${NC}"
echo $PRODUCTS | jq '.' | head -20
echo ""

# 6. Informar sobre o scheduler
echo -e "${YELLOW}6️⃣  Aguardando scheduler (30 segundos)...${NC}"
echo "ℹ️  O scheduler está configurado para rodar a cada 10 segundos"
echo "ℹ️  Com 3 tentativas paralelas, aguardando 30 segundos..."
for i in {30..1}; do
  echo -ne "\r⏳ Aguardando... ${i}s"
  sleep 1
done
echo -e "\r${GREEN}✓ Tempo decorrido${NC}\n"

# 7. Consultar histórico do primeiro produto
echo -e "${YELLOW}7️⃣  Consultando histórico de preços (Produto 1)...${NC}"
HISTORY=$(curl -s -X GET $BASE_URL/products/$PRODUCT1_ID/history \
  -H "Authorization: Bearer $TOKEN")

HISTORY_COUNT=$(echo $HISTORY | jq 'length')
if [ "$HISTORY_COUNT" -eq 0 ]; then
  echo -e "${RED}✗ Nenhum snapshot encontrado no histórico!${NC}"
  echo "ℹ️  Possíveis motivos:"
  echo "   - O scheduler ainda não foi executado"
  echo "   - Houve erro ao fazer scraping da URL"
  echo "   - O DynamoDB não está funcionando"
  echo ""
  echo "Verifique os logs da aplicação para mais detalhes:"
  echo "   grep -i 'scraping\|preço\|scheduler' logs/app.log"
else
  echo -e "${GREEN}✓ Histórico encontrado! Total de snapshots: $HISTORY_COUNT${NC}"
  echo $HISTORY | jq '.'
fi
echo ""

# 8. Resumo
echo "============================================"
echo -e "${GREEN}✓ TESTE CONCLUÍDO${NC}"
echo "============================================"
echo ""
echo "Resumo:"
echo "  • Email: $EMAIL"
echo "  • Produto 1 ID: $PRODUCT1_ID"
echo "  • Produto 2 ID: $PRODUCT2_ID"
echo "  • Snapshots encontrados: $HISTORY_COUNT"
echo ""
echo "Próximos passos:"
echo "  1. Aguarde mais alguns ciclos do scheduler (múltiplos de 10s)"
echo "  2. Execute este script novamente para ver a evolução"
echo "  3. Verifique os logs: tail -f logs/app.log"
echo ""

