# PriceTrackerSpringBoot - Steam Game Price Tracker

Um sistema de rastreamento de preços em tempo real para jogos da Steam. A aplicação monitora automaticamente variações de preços de jogos de seu interesse e mantém um histórico completo para análise de tendências.

---

## Indice

1. Visao Geral
2. Arquitetura Tecnica
3. Tecnologias Utilizadas
4. Estrutura do Projeto
5. Guia de Instalacao
6. Modelos de Dados
7. Endpoints da API
8. Autenticacao e Seguranca
9. Sistema de Scraping Steam
10. Execucao com Docker
11. Variaveis de Ambiente
12. Guia de Desenvolvimento

---

## 1. Visao Geral

### Proposito

PriceTrackerSpringBoot é uma API REST que permite rastrear preços de jogos da plataforma Steam. O sistema coleta automaticamente preços de jogos em intervalos regulares e oferece endpoints para gerenciar produtos monitorados e consultar históricos de preço.

### Funcionalidades Principais

- Registro e autenticacao de usuarios via JWT
- Adicionar jogos Steam ao monitoramento por URL
- Coleta automatica de precos via web scraping
- Armazenamento de historico completo de precos
- Consulta de tendências de preco
- Isolamento de dados por usuario (multi-tenancy)
- Documentacao interativa via Swagger/OpenAPI

### Casos de Uso

- Consumidor quer saber quando um jogo desejado cai de preco
- Analisar tendência de preco de um jogo ao longo do tempo
- Monitorar multiplos jogos simultaneamente
- Tomar decisão de compra baseada em histórico

---

## 2. Arquitetura Tecnica

### Fluxo de Sistema

```
Cliente (Web/Mobile/API)
    |
    v
Spring Boot REST API (Porta 8080)
    |
    +-- JwtAuthFilter
    |   (Validacao de token JWT)
    |
    +-- AuthController
    |   (Registro e Login)
    |
    +-- ProductController
    |   (Gerenciamento de jogos)
    |   |
    |   +-- ProductService
    |       |
    |       +-- ProductRepository (CRUD de jogos)
    |       +-- SnapshotRepository (Historico de precos)
    |
    +-- ScraperScheduler
    |   (Task agendada - executa a cada hora)
    |   |
    |   +-- SteamPriceFetcher
    |       |
    |       +-- PriceScraper (Parsing HTML)
    |
    v
AWS DynamoDB
    |
    +-- Tabela: AppUser
    +-- Tabela: Product
    +-- Tabela: PriceSnapshot
```

### Padroes de Arquitetura

- **Service Layer**: Separacao clara entre logica de negocio e controllers
- **Repository Pattern**: Abstração de acesso a dados com DynamoDB
- **Security Filter**: Interceptacao e validacao de JWT
- **Scheduler Pattern**: Execucao periodica de tarefas de scraping
- **Multi-tenancy**: Isolamento de dados por usuario

---

## 3. Tecnologias Utilizadas

### Stack Tecnologico

| Tecnologia | Versao | Proposito |
|---|---|---|
| Java | 21 | Linguagem de programacao |
| Spring Boot | 3.3.0 | Framework web |
| Spring Security | 3.3.0 | Autenticacao/Autorizacao |
| AWS DynamoDB SDK | 2.25.40 | Banco de dados NoSQL |
| JJWT | 0.12.5 | Geracao/Validacao JWT |
| Jsoup | 1.17.2 | Web scraping HTML |
| Springdoc OpenAPI | 2.5.0 | Documentacao Swagger |
| Lombok | 1.18.30 | Reducao de boilerplate |
| Maven | 3.9.6 | Gerenciador de dependencias |
| Docker | - | Containerizacao |

---

## 4. Estrutura do Projeto

```
PriceTrackerSpringBoot/
├── src/
│   ├── main/
│   │   ├── java/com/pricetracker/
│   │   │   ├── DemoApplication.java
│   │   │   │   (Ponto de entrada da aplicacao)
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── DynamoDbConfig.java
│   │   │   │   │   (Configuracao do DynamoDB)
│   │   │   │   └── SecurityConfig.java
│   │   │   │       (Configuracao de seguranca)
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── AppUser.java
│   │   │   │   │   │   (Entidade de usuario)
│   │   │   │   │   ├── Product.java
│   │   │   │   │   │   (Entidade de jogo Steam)
│   │   │   │   │   └── PriceSnapshot.java
│   │   │   │   │       (Entidade de historico de preco)
│   │   │   │   └── repository/
│   │   │   │       ├── UserRepository.java
│   │   │   │       ├── ProductRepository.java
│   │   │   │       └── SnapshotRepository.java
│   │   │   │
│   │   │   ├── scraper/
│   │   │   │   ├── GamePriceFetcher.java
│   │   │   │   │   (Interface para fetchers de preco)
│   │   │   │   ├── SteamPriceFetcher.java
│   │   │   │   │   (Implementacao especifica para Steam)
│   │   │   │   ├── PriceScraper.java
│   │   │   │   │   (Parsing HTML e extracao de precos)
│   │   │   │   └── ScraperScheduler.java
│   │   │   │       (Task agendada para coleta de precos)
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   │   (Filtro de autenticacao JWT)
│   │   │   │   └── JwtService.java
│   │   │   │       (Geracao e validacao de tokens)
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── AuthService.java
│   │   │   │   │   (Logica de autenticacao)
│   │   │   │   ├── ProductService.java
│   │   │   │   │   (Logica de gerenciamento de jogos)
│   │   │   │   └── NotFoundException.java
│   │   │   │       (Excecao customizada)
│   │   │   │
│   │   │   └── web/
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   │   (Endpoints de autenticacao)
│   │   │       │   └── ProductController.java
│   │   │       │       (Endpoints de gerenciamento de jogos)
│   │   │       ├── dto/
│   │   │       │   ├── LoginRequest.java
│   │   │       │   ├── LoginResponse.java
│   │   │       │   ├── AddProductRequest.java
│   │   │       │   └── (outros DTOs)
│   │   │       └── handler/
│   │   │           └── ApiExceptionHandler.java
│   │   │               (Tratamento global de erros)
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │           (Configuracoes da aplicacao)
│   │
│   └── test/java/
│       (Testes unitarios e de integracao)
│
├── docker-compose.yml
│   (Orquestracao de containers)
├── Dockerfile
│   (Imagem Docker da aplicacao)
├── pom.xml
│   (Configuracoes Maven e dependencias)
└── README.md
    (Este arquivo)
```

---

## 5. Guia de Instalacao

### 5.1 Requisitos

- Java 21 ou superior
- Maven 3.9.6 ou superior
- Docker e Docker Compose (para execucao containerizada)
- Git

### 5.2 Instalacao Local (sem Docker)

```bash
# 1. Clonar repositorio
git clone https://github.com/Emersonl33/PriceTrackerSpringBoot.git
cd PriceTrackerSpringBoot

# 2. Instalar dependencias Maven
mvn clean install

# 3. Executar aplicacao
mvn spring-boot:run
```

A aplicacao iniciara em http://localhost:8080

### 5.3 Instalacao com Docker Compose

```bash
# 1. Navegar para o diretorio do projeto
cd PriceTrackerSpringBoot

# 2. Construir e iniciar containers
docker-compose up -d

# 3. Verificar status dos containers
docker-compose ps

# 4. Acessar servicos:
#    - API: http://localhost:8080
#    - Swagger UI: http://localhost:8080/swagger-ui.html
#    - DynamoDB Admin: http://localhost:8001
```

### 5.4 Parar Containers

```bash
# Parar containers preservando volumes
docker-compose down

# Parar containers e remover volumes
docker-compose down -v
```

### 5.5 Verificar Saude da Aplicacao

```bash
curl http://localhost:8080/actuator/health
```

Resposta esperada:
```json
{
  "status": "UP"
}
```

---

## 6. Modelos de Dados

### 6.1 AppUser - Entidade de Usuario

Armazena informacoes de usuarios registrados no sistema.

```java
@DynamoDbBean
public class AppUser {
    private String id;                  // Chave primaria - ID unico
    private String email;               // Email unico para autenticacao
    private String passwordHash;        // Hash bcrypt da senha
    private String role;                // ADMIN ou USER
}
```

**Tabela DynamoDB**: `AppUser`
- Chave Primaria: `id` (String)
- Indice Global Secundario: `email` (para busca por email)

### 6.2 Product - Entidade de Jogo Steam

Representa um jogo Steam que o usuario deseja monitorar.

```java
@DynamoDbBean
public class Product {
    private String tenantId;            // Partition Key - ID do usuario dono
    private String productId;           // Sort Key - ID unico do jogo
    private String url;                 // URL do jogo na Steam Store
    private String name;                // Nome do jogo
    private BigDecimal currentPrice;    // Preco atual em reais
    private String createdAt;           // Timestamp de criacao (ISO-8601)
    private String updatedAt;           // Timestamp de ultima atualizacao (ISO-8601)
}
```

**Tabela DynamoDB**: `Product`
- Chave Composita:
  - Partition Key (PK): `tenantId` - Isola dados por usuario
  - Sort Key (SK): `productId` - ID unico do jogo por usuario

**Exemplo de URL Steam**:
```
https://store.steampowered.com/app/1790600/DRAGON_BALL_SPARKING_ZERO/
```

### 6.3 PriceSnapshot - Historico de Precos

Registra cada coleta de preco para analise de tendências.

```java
@DynamoDbBean
public class PriceSnapshot {
    private String productId;           // Partition Key - ID do jogo
    private String capturedAt;          // Sort Key - Timestamp ISO-8601
    private BigDecimal price;           // Preco capturado naquele momento
}
```

**Tabela DynamoDB**: `PriceSnapshot`
- Chave Composita:
  - Partition Key (PK): `productId` - Agrupa todos os precos de um jogo
  - Sort Key (SK): `capturedAt` - Ordena cronologicamente

**Exemplo de Snapshot**:
```json
{
  "productId": "1790600",
  "capturedAt": "2026-04-19T22:35:00Z",
  "price": 141.25
}
```

---

## 7. Endpoints da API

### 7.1 Autenticacao

#### POST /auth/register
Registra novo usuario na plataforma.

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "senha123"
  }'
```

**Resposta** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQGV4YW1wbGUuY29tIiwidGVuYW50SWQiOiJ1c2VyLWFiYzEyMyIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzEzNjAwMDAwLCJleHAiOjE3MTM2ODY0MDB9.xyz"
}
```

#### POST /auth/register/admin
Registra novo administrador com chave secreta.

```bash
curl -X POST http://localhost:8080/auth/register/admin \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "senhaadmin123",
    "adminSecret": "123456"
  }'
```

**Resposta** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

#### POST /auth/login
Autentica usuario e retorna JWT.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "senha123"
  }'
```

**Resposta** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQGV4YW1wbGUuY29tIiwi..."
}
```

#### GET /auth/users
Lista todos os usuarios registrados (DEBUG/ADMIN).

```bash
curl -X GET http://localhost:8080/auth/users \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Resposta** (200 OK):
```json
[
  {
    "id": "user-abc123",
    "email": "usuario@example.com",
    "passwordHash": "$2a$10$...",
    "role": "USER"
  }
]
```

### 7.2 Gerenciamento de Jogos

#### POST /products
Adiciona novo jogo Steam para monitorar.

```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DRAGON BALL SPARKING ZERO",
    "url": "https://store.steampowered.com/app/1790600/DRAGON_BALL_SPARKING_ZERO/"
  }'
```

**Resposta** (200 OK):
```json
{
  "tenantId": "user-abc123",
  "productId": "1790600",
  "name": "DRAGON BALL SPARKING ZERO",
  "url": "https://store.steampowered.com/app/1790600/DRAGON_BALL_SPARKING_ZERO/",
  "currentPrice": null,
  "createdAt": "2026-04-19T22:30:00Z",
  "updatedAt": "2026-04-19T22:30:00Z"
}
```

#### GET /products
Lista todos os jogos adicionados pelo usuario.

```bash
curl -X GET http://localhost:8080/products \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Resposta** (200 OK):
```json
[
  {
    "tenantId": "user-abc123",
    "productId": "1790600",
    "name": "DRAGON BALL SPARKING ZERO",
    "url": "https://store.steampowered.com/app/1790600/DRAGON_BALL_SPARKING_ZERO/",
    "currentPrice": 141.25,
    "createdAt": "2026-04-19T22:30:00Z",
    "updatedAt": "2026-04-19T23:45:00Z"
  },
  {
    "tenantId": "user-abc123",
    "productId": "851850",
    "name": "DRAGON BALL Z KAKAROT",
    "url": "https://store.steampowered.com/app/851850/DRAGON_BALL_Z_KAKAROT/",
    "currentPrice": 74.17,
    "createdAt": "2026-04-19T23:00:00Z",
    "updatedAt": "2026-04-19T23:45:00Z"
  }
]
```

#### DELETE /products/{productId}
Remove um jogo do monitoramento.

```bash
curl -X DELETE http://localhost:8080/products/1790600 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Resposta**: 204 No Content

#### GET /products/{productId}/history
Recupera historico completo de precos de um jogo.

```bash
curl -X GET http://localhost:8080/products/1790600/history \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Resposta** (200 OK):
```json
[
  {
    "productId": "1790600",
    "capturedAt": "2026-04-19T22:35:00Z",
    "price": 141.25
  },
  {
    "productId": "1790600",
    "capturedAt": "2026-04-19T23:35:00Z",
    "price": 141.25
  },
  {
    "productId": "1790600",
    "capturedAt": "2026-04-20T00:35:00Z",
    "price": 134.45
  }
]
```

#### GET /products/history/search?name=DRAGON
Busca historico de precos por nome do jogo.

```bash
curl -X GET "http://localhost:8080/products/history/search?name=DRAGON" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Resposta** (200 OK):
```json
{
  "DRAGON BALL SPARKING ZERO": [
    {
      "productId": "1790600",
      "capturedAt": "2026-04-19T22:35:00Z",
      "price": 141.25
    }
  ],
  "DRAGON BALL Z KAKAROT": [
    {
      "productId": "851850",
      "capturedAt": "2026-04-19T23:35:00Z",
      "price": 74.17
    }
  ]
}
```

#### GET /products/admin/all
Lista TODOS os jogos de todos os usuarios (ADMIN apenas).

```bash
curl -X GET http://localhost:8080/products/admin/all \
  -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
```

**Resposta** (200 OK):
```json
[
  {
    "tenantId": "user-abc123",
    "productId": "1790600",
    "name": "DRAGON BALL SPARKING ZERO",
    "currentPrice": 141.25
  },
  {
    "tenantId": "user-xyz789",
    "productId": "851850",
    "name": "DRAGON BALL Z KAKAROT",
    "currentPrice": 74.17
  }
]
```

---

## 8. Autenticacao e Seguranca

### 8.1 JWT (JSON Web Token)

Tokens JWT sao utilizados para autenticacao stateless sem sessoes no servidor.

**Estrutura JWT**:
```
Header.Payload.Signature

Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "usuario@example.com",
  "tenantId": "user-abc123",
  "role": "USER",
  "iat": 1713600000,
  "exp": 1713686400
}

Signature: HMACSHA256(base64url(Header) + "." + base64url(Payload), SECRET)
```

### 8.2 Fluxo de Autenticacao

```
1. Usuario faz POST /auth/login (email + password)
   |
   v
2. AuthService busca usuario no banco de dados
   |
   v
3. Valida senha usando BCrypt
   |
   v
4. Se valido, JwtService gera token com tenantId e role
   |
   v
5. Token retornado ao cliente
   |
   v
6. Cliente armazena token localmente
   |
   v
7. Proximas requisicoes incluem: Authorization: Bearer <token>
   |
   v
8. JwtAuthFilter intercepta requisicao
   |
   v
9. Extrai e valida token (assinatura + expiração)
   |
   v
10. Se valido, carrega tenantId em SecurityContext
    |
    v
11. Controller acessa tenantId para isolar dados
```

### 8.3 Isolamento de Dados por Tenant

ProductService extrai tenantId do JWT automaticamente:

```java
private String currentTenantId() {
    return (String) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
}
```

Garantias:
- Usuario so consegue acessar seus proprios jogos
- Usuario so consegue deletar seus proprios jogos
- Buscas retornam dados do usuario autenticado
- Senhas sao hasheadas com BCrypt (nao sao armazenadas em texto plano)

### 8.4 Variáveis Sensiveis

Em producao, todas as chaves devem ser armazenadas seguramente:

```bash
JWT_SECRET: String aleatoria de 32+ caracteres
ADMIN_SECRET: Chave para registrar novos administradores
AWS_DYNAMODB_ACCESS_KEY: Chave de acesso AWS
AWS_DYNAMODB_SECRET_KEY: Chave secreta AWS
```

Usar AWS Secrets Manager ou HashiCorp Vault em producao.

---

## 9. Sistema de Scraping Steam

### 9.1 Arquitetura de Coleta de Precos

```
ScraperScheduler (executa a cada 1 hora)
    |
    v
Busca TODOS os jogos do banco de dados
    |
    v
Para cada jogo:
    |
    +-- Verifica se URL eh da Steam
    |
    +-- SteamPriceFetcher inicia scraping
    |   |
    |   +-- Faz requisicao HTTP para Steam Store
    |   |
    |   +-- PriceScraper faz parsing HTML
    |   |
    |   +-- Extrai preco em reais
    |
    +-- Se preco obtido com sucesso:
    |   |
    |   +-- Cria PriceSnapshot no historico
    |   |
    |   +-- Atualiza currentPrice do jogo
    |   |
    |   +-- Log INFO com resultado
    |
    +-- Se falha:
        |
        +-- Log WARN com detalhes do erro
```

### 9.2 GamePriceFetcher - Interface

Define contrato que fetchers de preco devem implementar.

```java
public interface GamePriceFetcher {
    Optional<BigDecimal> fetchPrice(String url);
    boolean supports(String url);
    String sourceName();
}
```

Metodos:
- `fetchPrice(String url)`: Retorna preco em BigDecimal ou Optional.empty()
- `supports(String url)`: Valida se fetcher pode processar esta URL
- `sourceName()`: Nome da fonte para logs

### 9.3 SteamPriceFetcher - Implementacao

```java
@Component
public class SteamPriceFetcher implements GamePriceFetcher {
    
    private static final Logger log = LoggerFactory.getLogger(SteamPriceFetcher.class);
    private final PriceScraper priceScraper;
    
    @Override
    public boolean supports(String url) {
        return url != null && url.contains("steampowered.com");
    }
    
    @Override
    public Optional<BigDecimal> fetchPrice(String url) {
        try {
            String appId = extractAppId(url);
            String steamApiUrl = "https://store.steampowered.com/api/appdetails?appids=" + appId + "&cc=br";
            
            // Faz requisicao HTTP
            // Extrai preco da resposta JSON
            // Converte para BRL se necessario
            
            return priceScraper.scrape(url);
        } catch (Exception e) {
            log.error("Erro ao buscar preco Steam: {}", url, e);
            return Optional.empty();
        }
    }
    
    @Override
    public String sourceName() {
        return "Steam";
    }
    
    private String extractAppId(String url) {
        // Extrai appId de URL como: /app/1790600/...
        // Exemplo: https://store.steampowered.com/app/1790600/ -> 1790600
    }
}
```

### 9.4 PriceScraper - Extracao de Preco

Utilitario que faz parsing HTML e extracao inteligente de precos.

**Estrategias de Extracao**:
1. Seletor CSS: `meta[property=product:price:amount]`
2. Seletor CSS: `[itemprop=price]`
3. Seletor CSS de Steam especifico
4. Regex no corpo HTML: Busca padrão "R$ XXXX,XX"

**Normalizacao de Precos**:
- Entrada: "R$ 141,25" ou "141.25" ou "141,25"
- Saida: BigDecimal com valor em reais
- Validacoes:
  - Minimo: R$ 0.50
  - Maximo: R$ 999.999,99
  - Rejeita se nao for numero valido

### 9.5 ScraperScheduler - Execucao Periodica

```java
@Component
public class ScraperScheduler {
    
    @Scheduled(fixedDelayString = "${scraper.interval.ms}")
    public void runScraping() {
        List<Product> all = productRepo.findAll();
        log.info("Scheduler iniciado — {} jogos para verificar", all.size());
        
        // Thread pool de 5 threads para paralelismo
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = all.stream()
                .map(p -> CompletableFuture.runAsync(() -> scrapeProduct(p), executor))
                .toList();
        
        // Aguarda conclusao de todas as tasks
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Scheduler finalizado");
    }
}
```

**Configuracao**:
- Intervalo padrao: 1 hora (3.600.000 ms)
- Pode ser ajustado via variavel: `SCRAPER_INTERVAL_MS`
- Thread pool: 5 threads simultaneas

**Log de Exemplo**:
```
2026-04-19T22:33:15.829Z INFO 1 --- Scheduler iniciado — 5 produtos para verificar
2026-04-19T22:33:16.050Z INFO 1 --- [Steam] DRAGON BALL SPARKING ZERO -> R$ 141.25
2026-04-19T22:33:16.064Z INFO 1 --- [Steam] DRAGON BALL Z KAKAROT -> R$ 74.17
2026-04-19T22:33:16.091Z INFO 1 --- Scheduler finalizado
```

---

## 10. Execucao com Docker

### 10.1 Componentes Docker

O arquivo `docker-compose.yml` orquestra 3 servicos:

#### 1. dynamodb-local

Emulador do DynamoDB para desenvolvimento local.

```yaml
image: amazon/dynamodb-local:latest
port: 8000
volume: dynamodb-data (persistencia de dados)
```

Funcionalidades:
- Armazena tabelas: AppUser, Product, PriceSnapshot
- Funciona offline sem credenciais AWS
- Dados persistem em volume Docker

#### 2. dynamodb-admin

Interface web para gerenciar dados do DynamoDB.

```yaml
image: aaronshaf/dynamodb-admin:latest
port: 8001
```

Acesso: http://localhost:8001

Funcionalidades:
- Visualizar tabelas e registros
- Executar queries
- Inserir/editar/deletar dados manualmente
- Util para debugging

#### 3. price-tracker

Aplicacao Spring Boot containerizada.

```yaml
build: . (cria imagem a partir de Dockerfile)
port: 8080
```

Acesso:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 10.2 Dockerfile - Build Multi-Stage

```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B      # Cache de dependencias
COPY src ./src
RUN mvn clean package -DskipTests -B  # Compila e cria JAR

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine    
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Beneficios:
- Imagem final reduzida (sem codigo fonte, sem Maven)
- Build mais rapido (cache de dependencias)
- JDK apenas em build stage, JRE na imagem final

### 10.3 Volumes e Persistencia

```yaml
volumes:
  dynamodb-data:
    driver: local
```

Dados do DynamoDB persistem em volume Docker. Mesmo após parar containers, dados continuam presentes. Proxima execucao de `docker-compose up` recupera dados.

Limpar completamente:
```bash
docker-compose down -v   # Remove volumes também
```

### 10.4 Variaveis de Ambiente em Docker

```yaml
environment:
  AWS_DYNAMODB_ENDPOINT: http://dynamodb-local:8000
  AWS_DYNAMODB_REGION: us-east-1
  AWS_DYNAMODB_ACCESS_KEY: local
  AWS_DYNAMODB_SECRET_KEY: local
  JWT_SECRET: mude-esta-chave-para-algo-seguro-em-producao-32chars
  JWT_EXPIRATION: 86400000
  SCRAPER_INTERVAL_MS: 10000
  ADMIN_SECRET: 123456
```

---

## 11. Variaveis de Ambiente

### 11.1 Configuracoes de Desenvolvimento

```bash
# DynamoDB Local
AWS_DYNAMODB_ENDPOINT=http://localhost:8000
AWS_DYNAMODB_REGION=us-east-1
AWS_DYNAMODB_ACCESS_KEY=local
AWS_DYNAMODB_SECRET_KEY=local

# JWT
JWT_SECRET=mude-esta-chave-para-algo-seguro-em-producao-com-32-caracteres
JWT_EXPIRATION=86400000  # 1 dia em millisegundos

# Scraper
SCRAPER_INTERVAL_MS=10000  # 10 segundos para testes

# Admin
ADMIN_SECRET=123456
```

### 11.2 Configuracoes de Producao

```bash
# AWS DynamoDB (em producao, usar credenciais AWS reais)
AWS_DYNAMODB_ENDPOINT=<nao definir - usa padrao AWS>
AWS_DYNAMODB_REGION=us-east-1
AWS_DYNAMODB_ACCESS_KEY=<chave da conta AWS>
AWS_DYNAMODB_SECRET_KEY=<chave secreta da conta AWS>

# JWT (usar valores seguros e aleatorios)
JWT_SECRET=<string-aleatoria-32-caracteres-minimo>
JWT_EXPIRATION=86400000  # 1 dia

# Scraper (executar a cada 1 hora)
SCRAPER_INTERVAL_MS=3600000

# Admin (usar valor fortemente seguro)
ADMIN_SECRET=<chave-segura-para-registro-admin>
```

### 11.3 Arquivo application.yml

```yaml
aws:
  dynamodb:
    endpoint: ${AWS_DYNAMODB_ENDPOINT:http://localhost:8000}
    region: ${AWS_DYNAMODB_REGION:us-east-1}
    accessKey: ${AWS_DYNAMODB_ACCESS_KEY:local}
    secretKey: ${AWS_DYNAMODB_SECRET_KEY:local}

jwt:
  secret: ${JWT_SECRET:mude-esta-chave-para-algo-seguro-em-producao-32chars}
  expiration: ${JWT_EXPIRATION:86400000}

scraper:
  interval:
    ms: ${SCRAPER_INTERVAL_MS:3600000}

admin:
  secret: ${ADMIN_SECRET:minha-chave-admin-secreta}
```

Sintaxe: `${VARIAVEL_AMBIENTE:valor_padrao_se_nao_definida}`

---

## 12. Guia de Desenvolvimento

### 12.1 Estrutura de Testes

```bash
# Executar todos os testes
mvn test

# Executar teste especifico
mvn test -Dtest=SteamPriceFetcherTest

# Executar com cobertura de codigo
mvn test jacoco:report
```

### 12.2 Build Local

```bash
# Build sem testes
mvn clean package -DskipTests

# Build com testes
mvn clean package

# Localizar JAR gerado
# Arquivo: target/spring-api-1.0-SNAPSHOT.jar
```

### 12.3 Build Docker

```bash
# Construir imagem
docker build -t price-tracker:latest .

# Rodar container isolado
docker run -p 8080:8080 \
  -e AWS_DYNAMODB_ENDPOINT=http://host.docker.internal:8000 \
  price-tracker:latest

# Push para registro (Docker Hub, ECR, etc)
docker tag price-tracker:latest seu-usuario/price-tracker:latest
docker push seu-usuario/price-tracker:latest
```

Niveis de Log:
- DEBUG: Informacoes detalhadas para diagnostico
- INFO: Eventos normais do sistema
- WARN: Situacoes anormais mas nao criticas
- ERROR: Erros que precisam atencao imediata

### 12.5 Adicionar Novo Jogo para Teste

```bash
# 1. Fazer login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "teste@example.com", "password": "senha123"}'

# Copiar token retornado

# 2. Adicionar jogo Steam
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Elden Ring",
    "url": "https://store.steampowered.com/app/570940/ELDEN_RING/"
  }'

# 3. Verificar preco capturado (aguarde 10 segundos do scheduler)
curl -X GET http://localhost:8080/products \
  -H "Authorization: Bearer <TOKEN>"

# 4. Consultar historico
curl -X GET http://localhost:8080/products/570940/history \
  -H "Authorization: Bearer <TOKEN>"
```

### 12.6 Swagger/OpenAPI

Documentacao interativa automaticamente gerada em:
- http://localhost:8080/swagger-ui.html

Funcionalidades:
- Visualizar todos os endpoints
- Testar requisicoes direto da interface
- Ver schemas de request/response
- Copiar comandos curl

---

## Conclusao

PriceTrackerSpringBoot é um sistema completo e prodution-ready para monitorar precos de jogos Steam. A arquitetura robusta, autenticacao segura via JWT, isolamento de dados multi-tenant e scraping automatizado fazem dela uma solução escalavel e confiavel.

Para contribuicoes, issues ou duvidas, consulte o repositorio GitHub:
https://github.com/Emersonl33/PriceTrackerSpringBoot
