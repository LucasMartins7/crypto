package com.cryptotrader.service;

import com.cryptotrader.entity.ApiKey;
import com.cryptotrader.entity.Trade;
import com.cryptotrader.entity.User;
import com.cryptotrader.exception.ExchangeException;
import com.cryptotrader.repository.ApiKeyRepository;
import com.cryptotrader.repository.TradeRepository;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TradingService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);
    
    @Autowired
    private ExchangeService exchangeService;
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @Value("${app.trading.limits.max-order-size:1000.00}")
    private BigDecimal maxOrderSize;
    
    @Value("${app.trading.limits.daily-volume:10000.00}")
    private BigDecimal dailyVolumeLimit;
    
    public Trade createOrder(User user, String exchange, String symbol, String orderType, 
                           String side, BigDecimal amount, BigDecimal price) throws ExchangeException {
        
        // Rate limiting check
        if (!rateLimitingService.tryConsumeTradingRequest(user.getId().toString())) {
            throw new ExchangeException("Trading rate limit exceeded. Please wait before placing another order.");
        }
        
        // Validate order parameters
        validateOrderParameters(user, exchange, symbol, orderType, side, amount, price);
        
        // Get API key for the exchange
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findActiveApiKeyByUserAndExchange(user, exchange.toLowerCase());
        if (!apiKeyOpt.isPresent()) {
            throw new ExchangeException("No active API key found for exchange: " + exchange);
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // Create trade record
        Trade trade = new Trade(user, exchange, symbol, orderType, side, amount, price);
        trade = tradeRepository.save(trade);
        
        try {
            // Place order on exchange
            String exchangeOrderId = exchangeService.placeOrder(apiKey, trade);
            
            // Update trade with exchange order ID
            trade.setExchangeOrderId(exchangeOrderId);
            trade.setStatus("PENDING");
            
            logger.info("Order placed successfully: {} {} {} {} on {}", 
                       side, amount, symbol, orderType, exchange);
            
            return tradeRepository.save(trade);
            
        } catch (Exception e) {
            // Mark trade as failed
            trade.markAsFailed(e.getMessage());
            tradeRepository.save(trade);
            
            logger.error("Failed to place order: {}", e.getMessage());
            throw new ExchangeException("Failed to place order: " + e.getMessage());
        }
    }
    
    public boolean cancelOrder(User user, Long tradeId) throws ExchangeException {
        Optional<Trade> tradeOpt = tradeRepository.findByIdAndUser(tradeId, user);
        if (!tradeOpt.isPresent()) {
            throw new ExchangeException("Trade not found or access denied");
        }
        
        Trade trade = tradeOpt.get();
        
        if (!trade.isPending()) {
            throw new ExchangeException("Cannot cancel order with status: " + trade.getStatus());
        }
        
        // Get API key for the exchange
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findActiveApiKeyByUserAndExchange(user, trade.getExchange());
        if (!apiKeyOpt.isPresent()) {
            throw new ExchangeException("No active API key found for exchange: " + trade.getExchange());
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        try {
            boolean cancelled = exchangeService.cancelOrder(apiKey, trade.getExchangeOrderId(), trade.getSymbol());
            
            if (cancelled) {
                trade.markAsCancelled();
                tradeRepository.save(trade);
                
                logger.info("Order cancelled successfully: {}", trade.getExchangeOrderId());
                return true;
            } else {
                logger.warn("Failed to cancel order: {}", trade.getExchangeOrderId());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling order {}: {}", trade.getExchangeOrderId(), e.getMessage());
            throw new ExchangeException("Failed to cancel order: " + e.getMessage());
        }
    }
    
    public Map<String, BigDecimal> getPortfolioBalance(User user, String exchange) throws ExchangeException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findActiveApiKeyByUserAndExchange(user, exchange.toLowerCase());
        if (!apiKeyOpt.isPresent()) {
            throw new ExchangeException("No active API key found for exchange: " + exchange);
        }
        
        return exchangeService.getAccountBalance(apiKeyOpt.get());
    }
    
    public Ticker getMarketData(User user, String exchange, String symbol) throws ExchangeException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findActiveApiKeyByUserAndExchange(user, exchange.toLowerCase());
        if (!apiKeyOpt.isPresent()) {
            throw new ExchangeException("No active API key found for exchange: " + exchange);
        }
        
        return exchangeService.getTicker(apiKeyOpt.get(), symbol);
    }
    
    public List<Trade> getUserTrades(User user) {
        return tradeRepository.findByUser(user);
    }
    
    public List<Trade> getUserTradesByExchange(User user, String exchange) {
        return tradeRepository.findByUserAndExchange(user, exchange.toLowerCase());
    }
    
    public Optional<Trade> getTradeById(User user, Long tradeId) {
        return tradeRepository.findByIdAndUser(tradeId, user);
    }
    
    private void validateOrderParameters(User user, String exchange, String symbol, String orderType, 
                                       String side, BigDecimal amount, BigDecimal price) throws ExchangeException {
        
        // Validate exchange
        if (!List.of("binance", "coinbase", "kraken").contains(exchange.toLowerCase())) {
            throw new ExchangeException("Unsupported exchange: " + exchange);
        }
        
        // Validate order type
        if (!List.of("MARKET", "LIMIT").contains(orderType.toUpperCase())) {
            throw new ExchangeException("Unsupported order type: " + orderType);
        }
        
        // Validate side
        if (!List.of("BUY", "SELL").contains(side.toUpperCase())) {
            throw new ExchangeException("Invalid order side: " + side);
        }
        
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExchangeException("Amount must be greater than zero");
        }
        
        // Validate price for limit orders
        if ("LIMIT".equals(orderType.toUpperCase()) && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ExchangeException("Price must be greater than zero for limit orders");
        }
        
        // Check order size limits
        if (amount.compareTo(maxOrderSize) > 0) {
            throw new ExchangeException("Order size exceeds maximum allowed: " + maxOrderSize);
        }
        
        // Check daily volume limits (simplified check)
        BigDecimal dailyVolume = calculateDailyVolume(user);
        BigDecimal estimatedOrderValue = price != null ? amount.multiply(price) : amount.multiply(BigDecimal.valueOf(50000)); // Rough estimate for market orders
        
        if (dailyVolume.add(estimatedOrderValue).compareTo(dailyVolumeLimit) > 0) {
            throw new ExchangeException("Order would exceed daily volume limit: " + dailyVolumeLimit);
        }
    }
    
    private BigDecimal calculateDailyVolume(User user) {
        // This is a simplified implementation
        // In a real application, you would calculate the actual daily volume
        BigDecimal buyVolume = tradeRepository.getTotalBuyVolumeByUser(user);
        BigDecimal sellVolume = tradeRepository.getTotalSellVolumeByUser(user);
        
        return (buyVolume != null ? buyVolume : BigDecimal.ZERO)
                .add(sellVolume != null ? sellVolume : BigDecimal.ZERO);
    }
}
