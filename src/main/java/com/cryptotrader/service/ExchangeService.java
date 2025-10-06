package com.cryptotrader.service;

import com.cryptotrader.entity.ApiKey;
import com.cryptotrader.entity.Trade;
import com.cryptotrader.entity.User;
import com.cryptotrader.exception.ExchangeException;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.coinbasepro.CoinbaseProExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.kraken.KrakenExchange;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExchangeService.class);
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Value("${app.exchanges.sandbox-mode:true}")
    private boolean sandboxMode;
    
    private final Map<String, Exchange> exchangeCache = new ConcurrentHashMap<>();
    
    private static final Map<String, Class<? extends Exchange>> SUPPORTED_EXCHANGES = Map.of(
        "binance", BinanceExchange.class,
        "coinbase", CoinbaseProExchange.class,
        "kraken", KrakenExchange.class
    );
    
    public Exchange getExchange(ApiKey apiKey) throws ExchangeException {
        String cacheKey = apiKey.getUser().getId() + "_" + apiKey.getExchange();
        
        return exchangeCache.computeIfAbsent(cacheKey, k -> {
            try {
                return createExchange(apiKey);
            } catch (Exception e) {
                logger.error("Failed to create exchange for {}: {}", apiKey.getExchange(), e.getMessage());
                throw new ExchangeException("Failed to initialize exchange: " + e.getMessage());
            }
        });
    }
    
    private Exchange createExchange(ApiKey apiKey) throws ExchangeException {
        Class<? extends Exchange> exchangeClass = SUPPORTED_EXCHANGES.get(apiKey.getExchange().toLowerCase());
        if (exchangeClass == null) {
            throw new ExchangeException("Unsupported exchange: " + apiKey.getExchange());
        }
        
        try {
            ExchangeSpecification spec = new ExchangeSpecification(exchangeClass);
            
            // Decrypt API credentials
            String decryptedApiKey = encryptionService.decrypt(apiKey.getEncryptedApiKey());
            String decryptedSecret = encryptionService.decrypt(apiKey.getEncryptedApiSecret());
            
            spec.setApiKey(decryptedApiKey);
            spec.setSecretKey(decryptedSecret);
            
            // Set passphrase for exchanges that require it (like Coinbase Pro)
            if (apiKey.getEncryptedPassphrase() != null) {
                String decryptedPassphrase = encryptionService.decrypt(apiKey.getEncryptedPassphrase());
                spec.setExchangeSpecificParametersItem("passphrase", decryptedPassphrase);
            }
            
            // Configure sandbox mode if enabled
            if (sandboxMode) {
                configureSandboxMode(spec, apiKey.getExchange());
            }
            
            Exchange exchange = ExchangeFactory.INSTANCE.createExchange(spec);
            
            // Test connection
            testConnection(exchange);
            
            return exchange;
            
        } catch (Exception e) {
            logger.error("Failed to create exchange {}: {}", apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to create exchange: " + e.getMessage());
        }
    }
    
    private void configureSandboxMode(ExchangeSpecification spec, String exchangeName) {
        switch (exchangeName.toLowerCase()) {
            case "binance":
                spec.setExchangeSpecificParametersItem("Use_Sandbox", true);
                break;
            case "coinbase":
                spec.setSslUri("https://api-public.sandbox.pro.coinbase.com/");
                break;
            // Kraken doesn't have a separate sandbox
        }
    }
    
    private void testConnection(Exchange exchange) throws IOException {
        try {
            AccountService accountService = exchange.getAccountService();
            accountService.getAccountInfo();
        } catch (Exception e) {
            logger.warn("Connection test failed, but exchange created: {}", e.getMessage());
            // Don't throw exception here as some exchanges might have different behavior
        }
    }
    
    public Map<String, BigDecimal> getAccountBalance(ApiKey apiKey) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            AccountService accountService = exchange.getAccountService();
            AccountInfo accountInfo = accountService.getAccountInfo();
            
            Map<String, BigDecimal> balances = new HashMap<>();
            for (Balance balance : accountInfo.getWallet().getBalances().values()) {
                if (balance.getAvailable().compareTo(BigDecimal.ZERO) > 0) {
                    balances.put(balance.getCurrency().getCurrencyCode(), balance.getAvailable());
                }
            }
            
            // Update last used timestamp
            apiKey.updateLastUsed();
            
            return balances;
            
        } catch (Exception e) {
            logger.error("Failed to get account balance for {}: {}", apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to get account balance: " + e.getMessage());
        }
    }
    
    public Ticker getTicker(ApiKey apiKey, String symbol) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            MarketDataService marketDataService = exchange.getMarketDataService();
            
            CurrencyPair currencyPair = parseCurrencyPair(symbol);
            return marketDataService.getTicker(currencyPair);
            
        } catch (Exception e) {
            logger.error("Failed to get ticker for {} on {}: {}", symbol, apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to get ticker: " + e.getMessage());
        }
    }
    
    public String placeOrder(ApiKey apiKey, Trade trade) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            TradeService tradeService = exchange.getTradeService();
            
            CurrencyPair currencyPair = parseCurrencyPair(trade.getSymbol());
            Order.OrderType orderType = "BUY".equals(trade.getSide()) ? Order.OrderType.BID : Order.OrderType.ASK;
            
            String orderId;
            
            if ("MARKET".equals(trade.getOrderType())) {
                MarketOrder marketOrder = new MarketOrder(orderType, trade.getAmount(), currencyPair);
                orderId = tradeService.placeMarketOrder(marketOrder);
            } else if ("LIMIT".equals(trade.getOrderType())) {
                LimitOrder limitOrder = new LimitOrder(orderType, trade.getAmount(), currencyPair, null, null, trade.getPrice());
                orderId = tradeService.placeLimitOrder(limitOrder);
            } else {
                throw new ExchangeException("Unsupported order type: " + trade.getOrderType());
            }
            
            // Update last used timestamp
            apiKey.updateLastUsed();
            
            return orderId;
            
        } catch (Exception e) {
            logger.error("Failed to place order on {}: {}", apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to place order: " + e.getMessage());
        }
    }
    
    public boolean cancelOrder(ApiKey apiKey, String orderId, String symbol) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            TradeService tradeService = exchange.getTradeService();
            
            CurrencyPair currencyPair = parseCurrencyPair(symbol);
            return tradeService.cancelOrder(orderId);
            
        } catch (Exception e) {
            logger.error("Failed to cancel order {} on {}: {}", orderId, apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to cancel order: " + e.getMessage());
        }
    }
    
    public OpenOrders getOpenOrders(ApiKey apiKey) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            TradeService tradeService = exchange.getTradeService();
            
            return tradeService.getOpenOrders();
            
        } catch (Exception e) {
            logger.error("Failed to get open orders for {}: {}", apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to get open orders: " + e.getMessage());
        }
    }
    
    public UserTrades getTradeHistory(ApiKey apiKey, int limit) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            TradeService tradeService = exchange.getTradeService();
            
            return tradeService.getTradeHistory(tradeService.createTradeHistoryParams());
            
        } catch (Exception e) {
            logger.error("Failed to get trade history for {}: {}", apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to get trade history: " + e.getMessage());
        }
    }
    
    public Set<CurrencyPair> getAvailableCurrencyPairs(ApiKey apiKey) throws ExchangeException {
        try {
            Exchange exchange = getExchange(apiKey);
            return exchange.getExchangeMetaData().getCurrencyPairs().keySet();
            
        } catch (Exception e) {
            logger.error("Failed to get currency pairs for {}: {}", apiKey.getExchange(), e.getMessage());
            throw new ExchangeException("Failed to get currency pairs: " + e.getMessage());
        }
    }
    
    public boolean testConnection(ApiKey apiKey) {
        try {
            getAccountBalance(apiKey);
            apiKey.updateTestConnection("SUCCESS");
            return true;
        } catch (Exception e) {
            apiKey.updateTestConnection("FAILED");
            logger.error("Connection test failed for {}: {}", apiKey.getExchange(), e.getMessage());
            return false;
        }
    }
    
    private CurrencyPair parseCurrencyPair(String symbol) {
        // Handle different symbol formats (e.g., "BTCUSDT", "BTC/USDT", "BTC-USDT")
        String normalizedSymbol = symbol.replace("/", "").replace("-", "").toUpperCase();
        
        // Common currency pairs mapping
        Map<String, CurrencyPair> commonPairs = Map.of(
            "BTCUSDT", CurrencyPair.BTC_USDT,
            "ETHUSDT", CurrencyPair.ETH_USDT,
            "BTCUSD", CurrencyPair.BTC_USD,
            "ETHUSD", CurrencyPair.ETH_USD,
            "BTCEUR", CurrencyPair.BTC_EUR,
            "ETHEUR", CurrencyPair.ETH_EUR
        );
        
        CurrencyPair pair = commonPairs.get(normalizedSymbol);
        if (pair != null) {
            return pair;
        }
        
        // Try to parse manually
        if (normalizedSymbol.length() >= 6) {
            String base = normalizedSymbol.substring(0, 3);
            String counter = normalizedSymbol.substring(3);
            return new CurrencyPair(Currency.getInstance(base), Currency.getInstance(counter));
        }
        
        throw new IllegalArgumentException("Invalid currency pair format: " + symbol);
    }
    
    public void clearExchangeCache(User user) {
        exchangeCache.entrySet().removeIf(entry -> entry.getKey().startsWith(user.getId() + "_"));
    }
    
    public void clearAllExchangeCache() {
        exchangeCache.clear();
    }
}
