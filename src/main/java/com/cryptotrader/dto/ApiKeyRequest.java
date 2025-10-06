package com.cryptotrader.dto;

import javax.validation.constraints.NotBlank;

public class ApiKeyRequest {
    
    @NotBlank(message = "Exchange is required")
    private String exchange;
    
    @NotBlank(message = "API key is required")
    private String apiKey;
    
    @NotBlank(message = "API secret is required")
    private String apiSecret;
    
    private String passphrase; // Optional, required for some exchanges like Coinbase Pro
    
    public ApiKeyRequest() {}
    
    public ApiKeyRequest(String exchange, String apiKey, String apiSecret) {
        this.exchange = exchange;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public ApiKeyRequest(String exchange, String apiKey, String apiSecret, String passphrase) {
        this(exchange, apiKey, apiSecret);
        this.passphrase = passphrase;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
    
    public String getPassphrase() {
        return passphrase;
    }
    
    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}
