# 🚀 Crypto Trading API - Java Spring Boot

Uma aplicação completa de trading de criptomoedas desenvolvida em Java com Spring Boot, oferecendo integração segura com múltiplas exchanges e funcionalidades avançadas de trading.

## 📋 Índice

- [Características](#-características)
- [Tecnologias](#-tecnologias)
- [Arquitetura](#-arquitetura)
- [Instalação](#-instalação)
- [Configuração](#-configuração)
- [Deploy](#-deploy)
- [API Endpoints](#-api-endpoints)
- [Segurança](#-segurança)
- [Monitoramento](#-monitoramento)
- [Desenvolvimento](#-desenvolvimento)

## ✨ Características

### 🔐 **Segurança Avançada**
- Autenticação JWT com refresh tokens
- Criptografia AES-256 para chaves de API
- Rate limiting inteligente
- Validação rigorosa de entrada
- Logs de segurança detalhados

### 📈 **Trading Profissional**
- Suporte a múltiplas exchanges (Binance, Coinbase, Kraken)
- Ordens Market e Limit
- Cancelamento de ordens
- Histórico completo de transações
- Limites de trading configuráveis

### 🏗️ **Arquitetura Robusta**
- Design patterns (Repository, Service, DTO)
- Tratamento de exceções centralizado
- Configuração por ambiente
- Logs estruturados
- Health checks

### 🌐 **Deploy Flexível**
- Docker containerizado
- Suporte a múltiplas plataformas cloud
- Configuração por variáveis de ambiente
- CI/CD ready

## 🛠️ Tecnologias

- **Java 11** - Linguagem principal
- **Spring Boot 2.7.18** - Framework web
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - Persistência de dados
- **H2/PostgreSQL** - Banco de dados
- **XChange** - Integração com exchanges
- **JWT** - Tokens de autenticação
- **Docker** - Containerização
- **Maven** - Gerenciamento de dependências

## 🏛️ Arquitetura

```
src/main/java/com/cryptotrader/
├── config/          # Configurações (Security, CORS, etc.)
├── controller/      # Controllers REST
├── dto/            # Data Transfer Objects
├── entity/         # Entidades JPA
├── exception/      # Exceções customizadas
├── repository/     # Repositórios de dados
├── security/       # Componentes de segurança
└── service/        # Lógica de negócio
```

### 📊 **Fluxo de Dados**

1. **Cliente** → Controller (validação)
2. **Controller** → Service (lógica de negócio)
3. **Service** → Repository (persistência)
4. **Service** → ExchangeService (integração)
5. **ExchangeService** → Exchange APIs

## 🚀 Instalação

### Pré-requisitos

- Java 11+
- Maven 3.6+
- Docker (opcional)

### 1. Clone o Repositório

```bash
git clone <repository-url>
cd crypto-trading-api
```

### 2. Instale Dependências

```bash
mvn clean install
```

### 3. Configure Variáveis de Ambiente

```bash
cp .env.example .env
# Edite o arquivo .env com suas configurações
```

### 4. Execute a Aplicação

```bash
# Desenvolvimento
mvn spring-boot:run

# Ou com Docker
docker-compose up -d
```

## ⚙️ Configuração

### Variáveis de Ambiente Obrigatórias

```bash
# Segurança JWT
APP_SECURITY_JWT_SECRET=sua_chave_secreta_muito_longa_e_segura
APP_SECURITY_JWT_EXPIRATION=86400000

# Criptografia
APP_SECURITY_ENCRYPTION_KEY=sua_chave_de_criptografia

# Banco de Dados (opcional)
DATABASE_URL=jdbc:postgresql://localhost:5432/cryptodb
SPRING_DATASOURCE_USERNAME=cryptouser
SPRING_DATASOURCE_PASSWORD=cryptopass
```

### Variáveis Opcionais

```bash
# Trading
APP_EXCHANGES_SANDBOX_MODE=true
APP_TRADING_LIMITS_MAX_ORDER_SIZE=1000.00
APP_TRADING_LIMITS_DAILY_VOLUME=10000.00

# Servidor
SERVER_PORT=8080
```

### Configuração de Exchanges

Para usar a aplicação, você precisa configurar chaves de API das exchanges:

1. **Binance**: API Key (64 chars) + Secret (64 chars)
2. **Coinbase Pro**: API Key (UUID) + Secret + Passphrase
3. **Kraken**: API Key (50+ chars) + Secret

## 🌐 Deploy

### Script Automatizado

```bash
./deploy.sh
```

### Opções de Deploy

#### 1. **Railway** (Recomendado)
```bash
# Instalar CLI
npm install -g @railway/cli

# Deploy
railway login
railway up
```

#### 2. **Render**
```bash
# Conecte seu repositório GitHub ao Render
# Use o arquivo render.yaml para configuração automática
```

#### 3. **Heroku**
```bash
# Instalar CLI
# https://devcenter.heroku.com/articles/heroku-cli

# Deploy
heroku create sua-app
git push heroku main
```

#### 4. **Docker**
```bash
# Build
docker build -t crypto-trading-api .

# Run
docker run -p 8080:8080 crypto-trading-api
```

## 📡 API Endpoints

### Autenticação
```http
POST /auth/register     # Registrar usuário
POST /auth/login        # Login
POST /auth/logout       # Logout
GET  /auth/validate     # Validar token
```

### Chaves de API
```http
GET    /api-keys        # Listar chaves
POST   /api-keys        # Adicionar chave
DELETE /api-keys/{id}   # Remover chave
POST   /api-keys/{id}/test  # Testar chave
```

### Trading
```http
POST   /trading/order           # Criar ordem
DELETE /trading/order/{id}      # Cancelar ordem
GET    /trading/portfolio/{exchange}  # Ver portfólio
GET    /trading/ticker/{exchange}/{symbol}  # Preço atual
GET    /trading/history         # Histórico
GET    /trading/stats          # Estatísticas
```

### Monitoramento
```http
GET /actuator/health    # Status da aplicação
GET /actuator/info      # Informações da aplicação
GET /actuator/metrics   # Métricas
```

## 🔒 Segurança

### Autenticação
- JWT tokens com expiração configurável
- Refresh tokens para sessões longas
- Rate limiting por IP e usuário

### Criptografia
- Chaves de API criptografadas com AES-256
- Senhas hasheadas com BCrypt
- Comunicação HTTPS obrigatória

### Rate Limiting
- API geral: 60 req/min por usuário
- Trading: 10 req/min por usuário
- Login: 5 tentativas por 15 min por IP
- Chaves API: 5 operações por 5 min por usuário

### Validação
- Validação de entrada em todos os endpoints
- Sanitização de dados
- Limites de trading configuráveis

## 📊 Monitoramento

### Health Checks
```bash
curl http://localhost:8080/actuator/health
```

### Logs
```bash
# Visualizar logs
docker-compose logs -f crypto-trading-api

# Localização dos logs
./logs/crypto-trading-api.log
```

### Métricas
- Endpoint: `/actuator/metrics`
- Prometheus compatible
- Custom metrics para trading

## 🔧 Desenvolvimento

### Executar Testes
```bash
mvn test
```

### Executar com Profile de Desenvolvimento
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Acessar Console H2 (desenvolvimento)
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:cryptodb
Username: sa
Password: (vazio)
```

### Estrutura de Testes
```
src/test/java/com/cryptotrader/
├── controller/     # Testes de controllers
├── service/       # Testes de serviços
├── repository/    # Testes de repositórios
└── integration/   # Testes de integração
```

## 📚 Documentação da API

Após iniciar a aplicação, acesse:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

## ⚠️ Aviso Legal

Esta aplicação permite transações reais de criptomoedas. Use com extrema cautela:

- ✅ Sempre teste em modo sandbox primeiro
- ✅ Configure limites de trading apropriados
- ✅ Mantenha suas chaves de API seguras
- ✅ Monitore suas transações regularmente
- ❌ Nunca compartilhe suas credenciais
- ❌ Não use em produção sem testes adequados

## 🆘 Suporte

- 📧 Email: support@cryptotrading.com
- 📖 Documentação: [Wiki do Projeto]
- 🐛 Issues: [GitHub Issues]
- 💬 Discussões: [GitHub Discussions]

---

**Desenvolvido com ❤️ para a comunidade de trading de criptomoedas**
