package com.cryptotrader.controller;

import com.cryptotrader.dto.ApiKeyRequest;
import com.cryptotrader.dto.ApiResponse;
import com.cryptotrader.entity.ApiKey;
import com.cryptotrader.entity.User;
import com.cryptotrader.repository.ApiKeyRepository;
import com.cryptotrader.service.EncryptionService;
import com.cryptotrader.service.ExchangeService;
import com.cryptotrader.service.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api-keys")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApiKeyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyController.class);
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Autowired
    private ExchangeService exchangeService;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    private static final List<String> SUPPORTED_EXCHANGES = List.of("binance", "coinbase", "kraken");
    
    @PostMapping
    public ResponseEntity<?> addApiKey(@Valid @RequestBody ApiKeyRequest request, 
                                     Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        // Rate limiting
        if (!rateLimitingService.tryConsumeApiKeyOperation(user.getId().toString())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Too many API key operations. Please wait before trying again."));
        }
        
        try {
            String exchange = request.getExchange().toLowerCase();
            
            // Validate exchange
            if (!SUPPORTED_EXCHANGES.contains(exchange)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Unsupported exchange: " + request.getExchange()));
            }
            
            // Check if user already has API key for this exchange
            if (apiKeyRepository.existsByUserAndExchange(user, exchange)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("API key for " + request.getExchange() + " already exists"));
            }
            
            // Validate API key format
            String validationError = validateApiKeyFormat(request.getApiKey(), request.getApiSecret(), exchange);
            if (validationError != null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(validationError));
            }
            
            // Encrypt sensitive data
            String encryptedApiKey = encryptionService.encrypt(request.getApiKey());
            String encryptedApiSecret = encryptionService.encrypt(request.getApiSecret());
            String encryptedPassphrase = null;
            
            if (request.getPassphrase() != null && !request.getPassphrase().trim().isEmpty()) {
                encryptedPassphrase = encryptionService.encrypt(request.getPassphrase());
            }
            
            // Create API key
            ApiKey apiKey = new ApiKey(user, exchange, encryptedApiKey, encryptedApiSecret, encryptedPassphrase);
            apiKey = apiKeyRepository.save(apiKey);
            
            logger.info("API key added for user {} on exchange {}", user.getUsername(), exchange);
            
            return ResponseEntity.ok(ApiResponse.success("API key added successfully", createApiKeyResponse(apiKey)));
            
        } catch (Exception e) {
            logger.error("Error adding API key for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to add API key"));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getApiKeys(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<ApiKey> apiKeys = apiKeyRepository.findByUserAndIsActive(user, true);
            
            List<Map<String, Object>> response = apiKeys.stream()
                    .map(this::createApiKeyResponse)
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            logger.error("Error retrieving API keys for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve API keys"));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteApiKey(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        // Rate limiting
        if (!rateLimitingService.tryConsumeApiKeyOperation(user.getId().toString())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Too many API key operations. Please wait before trying again."));
        }
        
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByIdAndUser(id, user);
            
            if (!apiKeyOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("API key not found"));
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            String exchange = apiKey.getExchange();
            
            // Clear exchange cache for this user
            exchangeService.clearExchangeCache(user);
            
            // Delete API key
            apiKeyRepository.delete(apiKey);
            
            logger.info("API key deleted for user {} on exchange {}", user.getUsername(), exchange);
            
            return ResponseEntity.ok(ApiResponse.success("API key deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Error deleting API key for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete API key"));
        }
    }
    
    @PostMapping("/{id}/test")
    public ResponseEntity<?> testApiKey(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        // Rate limiting
        if (!rateLimitingService.tryConsumeApiKeyOperation(user.getId().toString())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Too many API key operations. Please wait before trying again."));
        }
        
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByIdAndUser(id, user);
            
            if (!apiKeyOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("API key not found"));
            }
            
            ApiKey apiKey = apiKeyOpt.get();
            
            // Test connection
            boolean isValid = exchangeService.testConnection(apiKey);
            
            // Save test result
            apiKeyRepository.save(apiKey);
            
            if (isValid) {
                logger.info("API key test successful for user {} on exchange {}", user.getUsername(), apiKey.getExchange());
                return ResponseEntity.ok(ApiResponse.success("API key is working correctly"));
            } else {
                logger.warn("API key test failed for user {} on exchange {}", user.getUsername(), apiKey.getExchange());
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("API key test failed. Please check your credentials."));
            }
            
        } catch (Exception e) {
            logger.error("Error testing API key for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to test API key"));
        }
    }
    
    private String validateApiKeyFormat(String apiKey, String apiSecret, String exchange) {
        switch (exchange.toLowerCase()) {
            case "binance":
                if (apiKey.length() != 64) {
                    return "Binance API key should be 64 characters long";
                }
                if (apiSecret.length() != 64) {
                    return "Binance API secret should be 64 characters long";
                }
                break;
            case "coinbase":
                if (!apiKey.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
                    return "Coinbase API key should be in UUID format";
                }
                break;
            case "kraken":
                if (apiKey.length() < 50) {
                    return "Kraken API key should be at least 50 characters long";
                }
                break;
        }
        return null;
    }
    
    private Map<String, Object> createApiKeyResponse(ApiKey apiKey) {
        return Map.of(
            "id", apiKey.getId(),
            "exchange", apiKey.getExchange(),
            "createdAt", apiKey.getCreatedAt(),
            "lastUsed", apiKey.getLastUsed(),
            "isActive", apiKey.getIsActive(),
            "hasPassphrase", apiKey.hasPassphrase(),
            "testConnectionStatus", apiKey.getTestConnectionStatus() != null ? apiKey.getTestConnectionStatus() : "NOT_TESTED",
            "testConnectionAt", apiKey.getTestConnectionAt()
        );
    }
}
