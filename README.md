# Price Tracker Spring Boot

Um sistema de rastreamento de preços em tempo real construído com **Spring Boot 3.3**, **AWS DynamoDB**, **JWT Authentication** e **Web Scraping**.

## 📋 Descrição

Price Tracker é uma API REST que permite aos usuários rastrear preços de produtos em sites de e-commerce. O sistema coleta dados de preços periodicamente usando web scraping, armazena histórico de preços no DynamoDB e fornece endpoints para consultar variações de preço ao longo do tempo.

## ✨ Funcionalidades Principais

- **Autenticação JWT**: Sistema seguro de autenticação e autorização com tokens JWT
- **Cadastro de Produtos**: Registre URLs de produtos para rastreamento automático
- **Web Scraping Inteligente**: Extrai preços de múltiplos e-commerces (Mercado Livre, Amazon, etc.)
- **Histórico de Preços**: Mantenha registro completo das variações de preço
- **Multi-tenant**: Suporte para múltiplos usuários com dados isolados
- **Scheduler Automático**: Coleta de preços em intervalos configuráveis
- **Swagger/OpenAPI**: Documentação interativa da API

## 🚀 Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.3.0**
- **Spring Security** - Autenticação e autorização
- **Spring Web** - Endpoints REST
- **AWS SDK v2** - Integração com DynamoDB
- **JWT (JJWT)** - Tokens de autenticação
- **JSoup** - Web scraping
- **Lombok** - Redução de boilerplate
- **Springdoc OpenAPI** - Documentação Swagger
- **Maven** - Gerenciamento de dependências

## 📦 Estrutura do Projeto

```
src/main/java/com/pricetracker/
├── config/                 # Configurações (DynamoDB, Security)
├── domain/
│   ├── model/             # Entidades (Product, PriceSnapshot, AppUser)
│   └── repository/        # Repositórios DynamoDB
├── scraper/               # Componentes de web scraping
├── security/              # Autenticação JWT
├── service/               # Lógica de negócio
└── web/
    ├── controller/        # Endpoints REST
    ├── dto/              # Data Transfer Objects
    └── handler/          # Exception handlers
```

## 🔧 Configuração

### Pré-requisitos

- Java 21+
- Maven 3.8+
- AWS DynamoDB Local (para desenvolvimento) ou AWS DynamoDB em produção
- AWS Access Key ID e Secret Access Key

### Variáveis de Ambiente

```properties
AWS_ACCESS_KEY_ID=sua-chave-aqui
AWS_SECRET_ACCESS_KEY=sua-chave-secreta-aqui
```

### Configuração (application.yml)

```yaml
server:
  port: 8081

aws:
  dynamodb:
    endpoint: http://localhost:8000/  # DynamoDB Local
    region: us-east-1
    tables:
      table-products: Product
      table-price-history: PriceHistory

jwt:
  secret: sua-chave-secreta-com-minimo-32-caracteres
  expiration: 86400000  # 24 horas em ms

scraper:
  interval:
    ms: 10000  # 10 segundos
```

## 📚 Endpoints Principais

### Autenticação
- `POST /auth/register` - Registrar novo usuário
- `POST /auth/login` - Fazer login

### Produtos
- `POST /products` - Adicionar novo produto para rastreamento
- `GET /products` - Listar todos os produtos do usuário
- `DELETE /products/{productId}` - Remover produto
- `GET /products/{productId}/history` - Obter histórico de preços

### Documentação
- `GET /swagger-ui.html` - Documentação interativa Swagger

## 🎯 Casos de Uso

1. **Monitoramento de Preços**: Rastreie automaticamente preços de produtos desejados
2. **Análise de Tendências**: Visualize histórico de preços para identificar padrões
3. **Alertas de Preço**: Implemente notificações quando o preço atingir limites
4. **Comparação de Preços**: Rastreie o mesmo produto em múltiplos sites

## 🔐 Segurança

- Autenticação baseada em JWT
- Isolamento de dados por tenant
- Validação de entrada
- CORS configurado
- Senhas hasheadas (BCrypt)

## 🧪 Testes

```bash
# Executar todos os testes
mvn test

# Executar com cobertura
mvn test jacoco:report
```

## 📖 Instalação e Execução

### Clonando o repositório
```bash
git clone https://github.com/seu-usuario/PriceTrackerSpringBoot.git
cd PriceTrackerSpringBoot
```

### Instalando dependências
```bash
mvn clean install
```

### Executando a aplicação
```bash
mvn spring-boot:run
```

A API estará disponível em `http://localhost:8081`

## 🐳 Docker (Opcional)

```bash
# Build da imagem
docker build -t price-tracker:latest .

# Executar container
docker run -p 8081:8081 \
  -e AWS_ACCESS_KEY_ID=dummy \
  -e AWS_SECRET_ACCESS_KEY=dummy \
  price-tracker:latest
```

## 📝 Exemplos de Uso

### Registrar usuário
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "senha123"
  }'
```

### Fazer login
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "password": "senha123"
  }'
```

### Adicionar produto
```bash
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SEU_TOKEN_JWT" \
  -d '{
    "url": "https://www.mercadolivre.com.br/seu-produto",
    "name": "Notebook Samsung"
  }'
```

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 👤 Autor

**Emerson**

- GitHub: [@emerson](https://github.com/seu-usuario)

## 🔗 Links Úteis

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS DynamoDB](https://aws.amazon.com/dynamodb/)
- [JWT.io](https://jwt.io/)
- [JSoup](https://jsoup.org/)

## 📞 Suporte

Para questões, problemas ou sugestões, abra uma issue no repositório ou entre em contato.

---

**Desenvolvido com ❤️ por Emerson**

