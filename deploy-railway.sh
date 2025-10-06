#!/bin/bash

# Deploy script for Railway
echo "🚂 Deploying Crypto Trading API to Railway..."

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo -e "${RED}Railway CLI not found. Installing...${NC}"
    npm install -g @railway/cli
fi

# Login to Railway (if not already logged in)
echo -e "${BLUE}Checking Railway authentication...${NC}"
if ! railway whoami &> /dev/null; then
    echo -e "${BLUE}Please login to Railway:${NC}"
    railway login
fi

# Initialize Railway project if not exists
if [ ! -f "railway.json" ]; then
    echo -e "${BLUE}Initializing Railway project...${NC}"
    railway init
fi

# Set environment variables
echo -e "${BLUE}Setting environment variables...${NC}"
railway variables set SPRING_PROFILES_ACTIVE=production
railway variables set APP_SECURITY_JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
railway variables set APP_SECURITY_ENCRYPTION_KEY="$(openssl rand -base64 32 | tr -d '\n')"
railway variables set APP_EXCHANGES_SANDBOX_MODE=true
railway variables set APP_TRADING_LIMITS_MAX_ORDER_SIZE=1000.00
railway variables set APP_TRADING_LIMITS_DAILY_VOLUME=10000.00

# Build the application
echo -e "${BLUE}Building application...${NC}"
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Build successful!${NC}"
else
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi

# Deploy to Railway
echo -e "${BLUE}Deploying to Railway...${NC}"
railway up

if [ $? -eq 0 ]; then
    echo -e "${GREEN}🎉 Deployment successful!${NC}"
    echo -e "${BLUE}Getting deployment URL...${NC}"
    RAILWAY_URL=$(railway domain)
    echo -e "${GREEN}Your API is deployed at: ${RAILWAY_URL}${NC}"
    echo -e "${BLUE}Health check: ${RAILWAY_URL}/actuator/health${NC}"
else
    echo -e "${RED}Deployment failed!${NC}"
    exit 1
fi

echo -e "${BLUE}📊 Checking deployment status...${NC}"
railway status

echo -e "${GREEN}✅ Deployment complete!${NC}"
echo ""
echo "🔗 Useful commands:"
echo "  railway logs    - View application logs"
echo "  railway status  - Check deployment status"
echo "  railway domain  - Get deployment URL"
echo "  railway open    - Open in browser"
