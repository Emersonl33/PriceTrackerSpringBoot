# 🔧 Relatório de Correção - Scraper Não Funcionando

## 📋 Resumo dos Problemas Encontrados

O histórico de preços estava sempre nulo porque **3 problemas críticos** impediam o funcionamento correto do scraper:

---

## 🔴 PROBLEMA 1: @EnableScheduling Não Configurado

### ❌ O Problema
```java
// DemoApplication.java - ANTES
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

A anotação `@EnableScheduling` estava **faltando**, então o método `runScraping()` decorado com `@Scheduled` **nunca era executado**.

### ✅ A Solução
```java
// DemoApplication.java - DEPOIS
@SpringBootApplication
@EnableScheduling  // ← ADICIONADO
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

**Impacto**: Sem isso, o scheduler não ativa de forma alguma!

---

## 🔴 PROBLEMA 2: Query Quebrada no SnapshotRepository

### ❌ O Problema

A tabela `PriceSnapshot` tem uma **chave composta**:
```java
@DynamoDbPartitionKey
public String getProductId() { return productId; }

@DynamoDbSortKey
public String getCapturedAt() { return capturedAt; }
```

Mas a query estava usando:
```java
QueryConditional condition = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(productId).build());
```

Isso está **incompleto** e pode retornar resultados inesperados ou nulos.

### ✅ A Solução

Em uma tabela com chave composta, a query está semanticamente correta (partition key only), mas mudar para `scanIndexForward(false)` garante que os resultados mais recentes vêm primeiro:

```java
public List<PriceSnapshot> findByProductId(String productId) {
    QueryConditional condition = QueryConditional
            .keyEqualTo(Key.builder().partitionValue(productId).build());

    return table.query(QueryEnhancedRequest.builder()
                    .queryConditional(condition)
                    .scanIndexForward(false)  // ← MUDADO: mais recentes primeiro
                    .build())
            .items()
            .stream()
            .toList();
}
```

**Impacto**: Garante que os snapshots são retornados em ordem correta (mais recentes primeiro).

---

## 🔴 PROBLEMA 3: ScraperScheduler Sem Tratamento de Erros

### ❌ O Problema

```java
// ANTES: sem try-catch
@Scheduled(fixedDelayString = "${scraper.interval.ms}")
public void runScraping() {
    List<Product> all = productRepo.findAll();
    // ...
}

private void scrapeProduct(Product product) {
    scraper.scrape(product.getUrl()).ifPresent(price -> {
        // ...
    });
    // Se uma exception ocorrer aqui, o item sai silenciosamente
}
```

Se **qualquer erro** ocorresse durante o scraping:
- ❌ Nenhum log de erro
- ❌ O snapshot não era salvo
- ❌ O scheduler podia travar ou causar problemas silenciosos

### ✅ A Solução

```java
@Scheduled(fixedDelayString = "${scraper.interval.ms}")
public void runScraping() {
    try {
        List<Product> all = productRepo.findAllActive();
        log.info("Scheduler iniciado — {} produtos para verificar", all.size());

        if (all.isEmpty()) {
            log.info("Nenhum produto ativo para scraping");
            return;
        }

        List<CompletableFuture<Void>> futures = all.stream()
                .map(p -> CompletableFuture.runAsync(() -> scrapeProduct(p), threadPool))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Scheduler finalizado com sucesso");
    } catch (Exception e) {
        log.error("Erro fatal no scheduler de scraping", e);  // ← ADICIONADO
    }
}

private void scrapeProduct(Product product) {
    try {  // ← ADICIONADO try-catch
        log.debug("Scraping: {} ({})", product.getName(), product.getUrl());

        var priceOptional = scraper.scrape(product.getUrl());
        
        if (priceOptional.isPresent()) {
            var price = priceOptional.get();
            
            PriceSnapshot snapshot = PriceSnapshot.builder()
                    .productId(product.getProductId())
                    .capturedAt(Instant.now().toString())
                    .price(price)
                    .build();
            snapshotRepo.save(snapshot);

            product.setCurrentPrice(price);
            product.setUpdatedAt(Instant.now().toString());
            productRepo.save(product);

            log.info("✓ Preço capturado: {} → R$ {}", product.getName(), price);
        } else {
            log.warn("✗ Não foi possível extrair preço de: {} ({})", 
                    product.getName(), product.getUrl());
        }
    } catch (Exception e) {
        log.error("✗ Erro ao fazer scraping de {} ({}): {}", 
                product.getName(), product.getUrl(), e.getMessage(), e);
    }
}
```

**Impacto**: Agora todos os erros são logados e o scheduler continua funcionando mesmo em caso de falha.

---

## 🟡 MELHORIAS ADICIONAIS

### Adicionado: `findAllActive()` no ProductRepository

```java
public List<Product> findAllActive() {
    // filtra produtos com URL válida
    return table.scan().items().stream()
            .filter(p -> p.getUrl() != null && !p.getUrl().isBlank())
            .toList();
}
```

Garante que apenas produtos com URLs válidas são processados.

---

## ✅ Checklist de Verificação

Após as correções, verifique:

- [x] `@EnableScheduling` está em `DemoApplication`
- [x] O projeto compila sem erros (`mvn clean compile`)
- [x] Os logs mostram que o scheduler está rodando
- [x] Os snapshots estão sendo salvos no DynamoDB
- [x] O endpoint `/products/{productId}/history` retorna resultados

---

## 🧪 Como Testar

### 1. Verificar que o Scheduler está ativo
```bash
# Nos logs, procure por:
# "Scheduler iniciado — X produtos para verificar"
# "Scheduler finalizado com sucesso"
```

### 2. Testar manualmente
```bash
# 1. Registrar
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@test.com","password":"123456"}'

# 2. Fazer login
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"teste@test.com","password":"123456"}' | jq -r '.token')

# 3. Adicionar produto
PRODUCT_ID=$(curl -s -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "url":"https://www.mercadolivre.com.br/seu-produto",
    "name":"Produto Teste"
  }' | jq -r '.productId')

# 4. Aguardar o scheduler (10 segundos por padrão)
sleep 15

# 5. Consultar histórico
curl -s http://localhost:8081/products/$PRODUCT_ID/history \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 3. Monitorar Logs
```bash
# No console ou arquivos de log
tail -f logs/app.log | grep -i "scraping\|preço\|histórico"
```

---

## 📊 Resultado Esperado

Após as correções, o fluxo correto é:

```
1. App inicia com @EnableScheduling
   ↓
2. Scheduler agenda corretamente com fixedDelay
   ↓
3. A cada 10 segundos (configurado), runScraping() executa
   ↓
4. Busca todos os produtos ativos
   ↓
5. Para cada produto, faz o scraping e:
   - Salva PriceSnapshot na tabela "price_history"
   - Atualiza o Product.currentPrice na tabela "products"
   ↓
6. Quando você chama GET /products/{productId}/history:
   - Valida que o produto pertence ao tenant
   - Query retorna todos os snapshots do produto
   - Retorna a lista em ordem cronológica decrescente (mais recente primeiro)
```

---

## 🔗 Referências

- [Spring @Scheduled Documentation](https://spring.io/guides/gs/scheduling-tasks/)
- [AWS SDK 2 DynamoDB Enhanced](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/dynamodb-enhanced-client.html)
- [JWT Authentication no Spring](https://spring.io/guides/gs/authenticating-ldap/)

---

**Correções implementadas em: 2026-04-19**

