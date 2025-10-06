package com.cryptotrader.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @NotBlank(message = "Exchange is required")
    @Column(nullable = false)
    private String exchange;
    
    @NotBlank(message = "API key is required")
    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    @JsonIgnore
    private String encryptedApiKey;
    
    @NotBlank(message = "API secret is required")
    @Column(name = "encrypted_api_secret", nullable = false, columnDefinition = "TEXT")
    @JsonIgnore
    private String encryptedApiSecret;
    
    @Column(name = "encrypted_passphrase", columnDefinition = "TEXT")
    @JsonIgnore
    private String encryptedPassphrase;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "last_used")
    private LocalDateTime lastUsed;
    
    @Column(name = "test_connection_status")
    private String testConnectionStatus;
    
    @Column(name = "test_connection_at")
    private LocalDateTime testConnectionAt;
    
    // Constructors
    public ApiKey() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ApiKey(User user, String exchange, String encryptedApiKey, String encryptedApiSecret) {
        this();
        this.user = user;
        this.exchange = exchange.toLowerCase();
        this.encryptedApiKey = encryptedApiKey;
        this.encryptedApiSecret = encryptedApiSecret;
    }
    
    public ApiKey(User user, String exchange, String encryptedApiKey, String encryptedApiSecret, String encryptedPassphrase) {
        this(user, exchange, encryptedApiKey, encryptedApiSecret);
        this.encryptedPassphrase = encryptedPassphrase;
    }
    
    // Business methods
    public void updateLastUsed() {
        this.lastUsed = LocalDateTime.now();
    }
    
    public void updateTestConnection(String status) {
        this.testConnectionStatus = status;
        this.testConnectionAt = LocalDateTime.now();
    }
    
    public boolean hasPassphrase() {
        return encryptedPassphrase != null && !encryptedPassphrase.trim().isEmpty();
    }
    
    public boolean isExpired() {
        // Consider API key expired if not used for 90 days
        return lastUsed != null && lastUsed.isBefore(LocalDateTime.now().minusDays(90));
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
    
    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }
    
    public void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
    }
    
    public String getEncryptedApiSecret() {
        return encryptedApiSecret;
    }
    
    public void setEncryptedApiSecret(String encryptedApiSecret) {
        this.encryptedApiSecret = encryptedApiSecret;
    }
    
    public String getEncryptedPassphrase() {
        return encryptedPassphrase;
    }
    
    public void setEncryptedPassphrase(String encryptedPassphrase) {
        this.encryptedPassphrase = encryptedPassphrase;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    public String getTestConnectionStatus() {
        return testConnectionStatus;
    }
    
    public void setTestConnectionStatus(String testConnectionStatus) {
        this.testConnectionStatus = testConnectionStatus;
    }
    
    public LocalDateTime getTestConnectionAt() {
        return testConnectionAt;
    }
    
    public void setTestConnectionAt(LocalDateTime testConnectionAt) {
        this.testConnectionAt = testConnectionAt;
    }
}
