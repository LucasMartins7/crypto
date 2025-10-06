package com.cryptotrader.exception;

public class ExchangeException extends RuntimeException {
    
    private String exchangeName;
    private String operation;
    
    public ExchangeException(String message) {
        super(message);
    }
    
    public ExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExchangeException(String message, String exchangeName, String operation) {
        super(message);
        this.exchangeName = exchangeName;
        this.operation = operation;
    }
    
    public ExchangeException(String message, Throwable cause, String exchangeName, String operation) {
        super(message, cause);
        this.exchangeName = exchangeName;
        this.operation = operation;
    }
    
    public String getExchangeName() {
        return exchangeName;
    }
    
    public String getOperation() {
        return operation;
    }
}
