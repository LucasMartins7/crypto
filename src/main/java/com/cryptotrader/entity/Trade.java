package com.cryptotrader.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {
    
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
    
    @NotBlank(message = "Symbol is required")
    @Column(nullable = false)
    private String symbol;
    
    @NotBlank(message = "Order type is required")
    @Column(name = "order_type", nullable = false)
    private String orderType; // MARKET, LIMIT
    
    @NotBlank(message = "Side is required")
    @Column(nullable = false)
    private String side; // BUY, SELL
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;
    
    @Column(precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(name = "filled_amount", precision = 20, scale = 8)
    private BigDecimal filledAmount = BigDecimal.ZERO;
    
    @Column(name = "average_price", precision = 20, scale = 8)
    private BigDecimal averagePrice;
    
    @Column(name = "total_cost", precision = 20, scale = 8)
    private BigDecimal totalCost;
    
    @Column(name = "fee_amount", precision = 20, scale = 8)
    private BigDecimal feeAmount;
    
    @Column(name = "fee_currency")
    private String feeCurrency;
    
    @NotBlank(message = "Status is required")
    @Column(nullable = false)
    private String status; // PENDING, FILLED, PARTIALLY_FILLED, CANCELLED, FAILED
    
    @Column(name = "exchange_order_id")
    private String exchangeOrderId;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "executed_at")
    private LocalDateTime executedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    // Constructors
    public Trade() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    public Trade(User user, String exchange, String symbol, String orderType, String side, BigDecimal amount) {
        this();
        this.user = user;
        this.exchange = exchange.toLowerCase();
        this.symbol = symbol.toUpperCase();
        this.orderType = orderType.toUpperCase();
        this.side = side.toUpperCase();
        this.amount = amount;
    }
    
    public Trade(User user, String exchange, String symbol, String orderType, String side, BigDecimal amount, BigDecimal price) {
        this(user, exchange, symbol, orderType, side, amount);
        this.price = price;
    }
    
    // Business methods
    public void markAsFilled(BigDecimal filledAmount, BigDecimal averagePrice, BigDecimal totalCost) {
        this.status = "FILLED";
        this.filledAmount = filledAmount;
        this.averagePrice = averagePrice;
        this.totalCost = totalCost;
        this.executedAt = LocalDateTime.now();
    }
    
    public void markAsPartiallyFilled(BigDecimal filledAmount, BigDecimal averagePrice) {
        this.status = "PARTIALLY_FILLED";
        this.filledAmount = filledAmount;
        this.averagePrice = averagePrice;
    }
    
    public void markAsCancelled() {
        this.status = "CANCELLED";
        this.cancelledAt = LocalDateTime.now();
    }
    
    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }
    
    public void setFee(BigDecimal feeAmount, String feeCurrency) {
        this.feeAmount = feeAmount;
        this.feeCurrency = feeCurrency;
    }
    
    public boolean isPending() {
        return "PENDING".equals(status);
    }
    
    public boolean isFilled() {
        return "FILLED".equals(status);
    }
    
    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(status);
    }
    
    public BigDecimal getRemainingAmount() {
        return amount.subtract(filledAmount != null ? filledAmount : BigDecimal.ZERO);
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
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public String getOrderType() {
        return orderType;
    }
    
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }
    
    public String getSide() {
        return side;
    }
    
    public void setSide(String side) {
        this.side = side;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getFilledAmount() {
        return filledAmount;
    }
    
    public void setFilledAmount(BigDecimal filledAmount) {
        this.filledAmount = filledAmount;
    }
    
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }
    
    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }
    
    public BigDecimal getTotalCost() {
        return totalCost;
    }
    
    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
    
    public BigDecimal getFeeAmount() {
        return feeAmount;
    }
    
    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }
    
    public String getFeeCurrency() {
        return feeCurrency;
    }
    
    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getExchangeOrderId() {
        return exchangeOrderId;
    }
    
    public void setExchangeOrderId(String exchangeOrderId) {
        this.exchangeOrderId = exchangeOrderId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
