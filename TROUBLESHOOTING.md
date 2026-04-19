# 🔍 Guia de Troubleshooting - Price Tracker Scraper

## 📋 Índice
1. [Problemas Resolvidos](#problemas-resolvidos)
2. [Como Verificar se as Correções Funcionam](#como-verificar)
3. [Problemas Comuns e Soluções](#problemas-comuns)
4. [Debug Manual](#debug-manual)

---

## ✅ Problemas Resolvidos

### ✓ Problema 1: Scheduler não estava ativado
- **Status**: ✅ CORRIGIDO
- **Solução**: Adicionada `@EnableScheduling` em `DemoApplication.java`
- **Verificação**: Procure por "Scheduler iniciado" nos logs

### ✓ Problema 2: Query do histórico retornava nulo
- **Status**: ✅ CORRIGIDO
- **Solução**: Otimizada query em `SnapshotRepository.findByProductId()`
- **Verificação**: `GET /products/{productId}/history` agora retorna lista

### ✓ Problema 3: Sem tratamento de erros no scraper
- **Status**: ✅ CORRIGIDO
- **Solução**: Adicionado try-catch em `ScraperScheduler`
- **Verificação**: Todos os erros são logados

---

## 🧪 Como Verificar se as Correções Funcionam

### Opção 1: Usar o Script de Teste (Recomendado)

**Windows PowerShell:**
```powershell
.\test-scraper.ps1
```

**Linux/Mac:**
```bash
chmod +x test-scraper.sh
./test-scraper.sh
```

### Opção 2: Teste Manual com cURL

#### Passo 1: Registrar usuário
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "senha123"
  }'
```

#### Passo 2: Fazer login e copiar o token
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teste@example.com",
    "password": "senha123"
  }' | jq '.token'
```

#### Passo 3: Adicionar produto
```bash
TOKEN="seu-token-aqui"
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "url": "https://www.mercadolivre.com.br/",
    "name": "Teste de Produto"
  }' | jq '.productId'
```

#### Passo 4: Aguardar scheduler
```bash
# Aguarde pelo menos 10 segundos (intervalo configurado)
sleep 15
```

#### Passo 5: Consultar histórico
```bash
PRODUCT_ID="seu-produto-id-aqui"
TOKEN="seu-token-aqui"
curl -X GET http://localhost:8081/products/$PRODUCT_ID/history \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## 🚨 Problemas Comuns e Soluções

### ❌ Problema: "Nenhum produto ativo para scraping"
**Causa**: Nenhum produto foi cadastrado

**Solução**:
1. Faça login com um usuário
2. Adicione pelo menos um produto com URL válida

### ❌ Problema: Histórico retorna lista vazia
**Causa**: O scheduler ainda não foi executado ou falhou

**Solução**:
1. Aguarde 10-20 segundos (tempo do scheduler)
2. Verifique os logs procurando por "Scraping" ou "Scheduler"
3. Verifique se a URL do produto é válida

### ❌ Problema: "ProductId not found" ao consultar histórico
**Causa**: ProductId inválido ou pertence a outro usuário

**Solução**:
1. Verifique o productId correto: `GET /products` (com token)
2. Use o token correto do usuário que criou o produto

### ❌ Problema: DynamoDB retorna erro de conexão
**Causa**: DynamoDB Local não está rodando

**Solução** (se usando local):
```bash
# Iniciar DynamoDB Local (Docker)
docker run -p 8000:8000 amazon/dynamodb-local:latest

# Ou instalar local:
# https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html
```

### ❌ Problema: Erro "Failed to scrape URL"
**Causa**: A URL não é acessível ou a estrutura HTML é diferente

**Solução**:
1. Verifique se a URL é válida e acessível
2. O scraper tenta vários seletores CSS, mas alguns sites podem precisar de customização
3. Verifique os logs para detalhes específicos do erro

---

## 🔧 Debug Manual

### 1. Verificar se o Scheduler está ativo

Procure nos logs pela mensagem:
```
INFO com.pricetracker.scraper.ScraperScheduler - Scheduler iniciado
```

Se não aparecer:
1. Verifique se `@EnableScheduling` está em `DemoApplication`
2. Aguarde um pouco (pode levar até 30 segundos iniciais)

### 2. Verificar se o Scraping está funcionando

Procure nos logs por:
```
DEBUG com.pricetracker.scraper.ScraperScheduler - Scraping: [Nome do Produto]
INFO com.pricetracker.scraper.ScraperScheduler - ✓ Preço capturado: [Produto] → R$ [Preço]
```

Se aparecer "✗" em vez de "✓":
- Verifique a URL do produto
- Verifique a estrutura HTML do site

### 3. Verificar se os Snapshots estão sendo salvos

Acesse a API com um cliente REST:
```
GET /products/{productId}/history
Headers: Authorization: Bearer {token}
```

Se vazio:
1. Verifique se o DynamoDB está rodando
2. Verifique os logs para erros de conexão

### 4. Verificar se o DynamoDB está criando as tabelas

Ao iniciar, você deve ver:
```
INFO com.pricetracker.config.DynamoDbConfig - Criando tabela 'products'...
INFO com.pricetracker.config.DynamoDbConfig - Criando tabela 'price_history'...
```

Se não aparecer:
1. Verifique se o endpoint do DynamoDB em `application.yml` está correto
2. Verifique as credenciais AWS

---

## 📊 Verificação de Saúde

### Health Check Completo

```bash
# 1. API está respondendo?
curl http://localhost:8081/actuator/health

# 2. Você consegue fazer login?
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test"}'

# 3. Swagger está disponível?
curl http://localhost:8081/swagger-ui.html
```

### Verificação de Logs

**Windows PowerShell:**
```powershell
# Ver últimas 50 linhas
Get-Content "target/logs/app.log" -Tail 50

# Monitorar em tempo real
Get-Content "target/logs/app.log" -Tail 10 -Wait
```

**Linux/Mac:**
```bash
# Ver últimas 50 linhas
tail -50 target/logs/app.log

# Monitorar em tempo real
tail -f target/logs/app.log
```

---

## 🎯 Checklist de Verificação

Antes de concluir que o scraper não funciona, verifique:

- [ ] App rodando sem erros: `mvn spring-boot:run`
- [ ] `@EnableScheduling` em `DemoApplication` ✅
- [ ] DynamoDB Local rodando (se local): `docker ps | grep dynamodb`
- [ ] Banco de dados criado: `GET /actuator/health`
- [ ] Usuário criado: `POST /auth/register`
- [ ] Token obtido: `POST /auth/login`
- [ ] Produto criado: `POST /products` (retorna productId)
- [ ] Aguardou 10+ segundos
- [ ] Histórico consultado: `GET /products/{productId}/history`
- [ ] Logs mostram "Scheduler iniciado" 
- [ ] Logs mostram "✓ Preço capturado" ou "✗" (para falhas esperadas)

---

## 📞 Se Nada Funcionar

1. **Reinicie tudo**:
   ```bash
   # Parar a app
   Ctrl+C
   
   # Limpar build
   mvn clean
   
   # Reiniciar
   mvn spring-boot:run
   ```

2. **Verifique a Configuração**:
   - Abra `src/main/resources/application.yml`
   - Verifique endpoint DynamoDB
   - Verifique porta (deve ser 8081)
   - Verifique JWT secret

3. **Teste com URL simples**:
   - Tente com: `https://httpbin.org/html`
   - Isso ajuda a descartar problemas de scraping de sites reais

4. **Habilite Debug Logging**:
   ```yaml
   # Adicione a application.yml:
   logging:
     level:
       com.pricetracker: DEBUG
       org.springframework: INFO
   ```

5. **Procure pela Correção no Código**:
   - `DemoApplication.java` deve ter `@EnableScheduling`
   - `ScraperScheduler.java` deve ter try-catch com logs
   - Compile novamente: `mvn clean compile`

---

## 📚 Arquivos Modificados

- ✅ `src/main/java/com/pricetracker/DemoApplication.java` - Adicionada `@EnableScheduling`
- ✅ `src/main/java/com/pricetracker/scraper/ScraperScheduler.java` - Melhorado tratamento de erros
- ✅ `src/main/java/com/pricetracker/domain/repository/SnapshotRepository.java` - Melhorada query
- ✅ `src/main/java/com/pricetracker/domain/repository/ProductRepository.java` - Adicionado `findAllActive()`

---

**Última atualização**: 2026-04-19
**Versão**: 1.0

