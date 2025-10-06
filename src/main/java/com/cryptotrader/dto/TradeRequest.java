package com.cryptotrader.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TradeRequest {
    
    @NotBlank(message = "Exchange is required")
    private String exchange;
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotBlank(message = "Order type is required")
    private String orderType; // MARKET, LIMIT
    
    @NotBlank(message = "Side is required")
    private String side; // BUY, SELL
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price; // Required for LIMIT orders
    
    public TradeRequest() {}
    
    public TradeRequest(String exchange, String symbol, String orderType, String side, BigDecimal amount) {
        this.exchange = exchange;
        this.symbol = symbol;
        this.orderType = orderType;
        this.side = side;
        this.amount = amount;
    }
    
    public TradeRequest(String exchange, String symbol, String orderType, String side, BigDecimal amount, BigDecimal price) {
        this(exchange, symbol, orderType, side, amount);
        this.price = price;
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
}
