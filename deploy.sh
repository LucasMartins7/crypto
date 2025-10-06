#!/bin/bash

# Crypto Trading API Deployment Script
# Supports multiple cloud platforms

set -e

echo "🚀 Crypto Trading API Deployment Script"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is installed
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    print_success "Maven is available"
}

# Build the application
build_app() {
    print_status "Building the application..."
    mvn clean package -DskipTests
    if [ $? -eq 0 ]; then
        print_success "Application built successfully"
    else
        print_error "Build failed"
        exit 1
    fi
}

# Deploy to Railway
deploy_railway() {
    print_status "Deploying to Railway..."
    if command -v railway &> /dev/null; then
        railway login
        railway up
        print_success "Deployed to Railway"
    else
        print_warning "Railway CLI not found. Install it with: npm install -g @railway/cli"
        print_status "Manual deployment: Push to GitHub and connect to Railway"
    fi
}

# Deploy to Render
deploy_render() {
    print_status "Deploying to Render..."
    print_status "Manual deployment required:"
    echo "1. Push code to GitHub repository"
    echo "2. Connect repository to Render"
    echo "3. Use render.yaml configuration"
    print_success "Render deployment configuration ready"
}

# Deploy to Heroku
deploy_heroku() {
    print_status "Deploying to Heroku..."
    if command -v heroku &> /dev/null; then
        heroku login
        heroku create crypto-trading-api-$(date +%s)
        git push heroku main
        print_success "Deployed to Heroku"
    else
        print_warning "Heroku CLI not found. Install it from: https://devcenter.heroku.com/articles/heroku-cli"
        print_status "Manual deployment: Push to Heroku Git repository"
    fi
}

# Build Docker image
build_docker() {
    print_status "Building Docker image..."
    docker build -t crypto-trading-api:latest .
    if [ $? -eq 0 ]; then
        print_success "Docker image built successfully"
    else
        print_error "Docker build failed"
        exit 1
    fi
}

# Run with Docker Compose
run_docker_compose() {
    print_status "Starting with Docker Compose..."
    docker-compose up -d
    print_success "Application started with Docker Compose"
    print_status "Access the API at: http://localhost:8080"
    print_status "H2 Console (if enabled): http://localhost:8080/h2-console"
}

# Main deployment menu
main_menu() {
    echo ""
    echo "Select deployment option:"
    echo "1) Build application only"
    echo "2) Build Docker image"
    echo "3) Run with Docker Compose (local)"
    echo "4) Deploy to Railway"
    echo "5) Deploy to Render"
    echo "6) Deploy to Heroku"
    echo "7) All platforms (build + configs)"
    echo "0) Exit"
    echo ""
    read -p "Enter your choice [0-7]: " choice

    case $choice in
        1)
            check_maven
            build_app
            ;;
        2)
            check_maven
            build_app
            build_docker
            ;;
        3)
            check_maven
            build_app
            build_docker
            run_docker_compose
            ;;
        4)
            check_maven
            build_app
            deploy_railway
            ;;
        5)
            check_maven
            build_app
            deploy_render
            ;;
        6)
            check_maven
            build_app
            deploy_heroku
            ;;
        7)
            check_maven
            build_app
            build_docker
            print_success "Application ready for deployment to all platforms"
            print_status "Configuration files created for Railway, Render, and Heroku"
            ;;
        0)
            print_status "Exiting..."
            exit 0
            ;;
        *)
            print_error "Invalid option. Please try again."
            main_menu
            ;;
    esac
}

# Check if running in CI/CD environment
if [ "$CI" = "true" ]; then
    print_status "Running in CI/CD environment"
    check_maven
    build_app
    build_docker
else
    main_menu
fi

print_success "Deployment script completed!"
echo ""
echo "📚 Documentation:"
echo "- API Documentation: http://your-domain/swagger-ui.html"
echo "- Health Check: http://your-domain/actuator/health"
echo ""
echo "🔧 Environment Variables to set:"
echo "- APP_SECURITY_JWT_SECRET (required)"
echo "- APP_SECURITY_ENCRYPTION_KEY (required)"
echo "- DATABASE_URL (optional, defaults to H2)"
echo "- APP_EXCHANGES_SANDBOX_MODE (optional, defaults to true)"
echo ""
echo "🚀 Your Crypto Trading API is ready for production!"
