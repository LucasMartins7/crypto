package com.cryptotrader.controller;

import com.cryptotrader.dto.ApiResponse;
import com.cryptotrader.dto.TradeRequest;
import com.cryptotrader.entity.Trade;
import com.cryptotrader.entity.User;
import com.cryptotrader.service.TradingService;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/trading")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TradingController {
    
    private static final Logger logger = LoggerFactory.getLogger(TradingController.class);
    
    @Autowired
    private TradingService tradingService;
    
    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@Valid @RequestBody TradeRequest request, 
                                       Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        try {
            Trade trade = tradingService.createOrder(
                user,
                request.getExchange(),
                request.getSymbol(),
                request.getOrderType(),
                request.getSide(),
                request.getAmount(),
                request.getPrice()
            );
            
            logger.info("Order created: {} {} {} {} on {}", 
                       request.getSide(), request.getAmount(), request.getSymbol(), 
                       request.getOrderType(), request.getExchange());
            
            return ResponseEntity.ok(ApiResponse.success("Order created successfully", createTradeResponse(trade)));
            
        } catch (Exception e) {
            logger.error("Error creating order for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/order/{tradeId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long tradeId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            boolean cancelled = tradingService.cancelOrder(user, tradeId);
            
            if (cancelled) {
                logger.info("Order cancelled successfully: {}", tradeId);
                return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to cancel order"));
            }
            
        } catch (Exception e) {
            logger.error("Error cancelling order {} for user {}: {}", tradeId, user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/portfolio/{exchange}")
    public ResponseEntity<?> getPortfolio(@PathVariable String exchange, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Map<String, BigDecimal> balance = tradingService.getPortfolioBalance(user, exchange);
            
            return ResponseEntity.ok(ApiResponse.success("Portfolio retrieved successfully", balance));
            
        } catch (Exception e) {
            logger.error("Error retrieving portfolio for user {} on {}: {}", user.getUsername(), exchange, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/ticker/{exchange}/{symbol}")
    public ResponseEntity<?> getTicker(@PathVariable String exchange, @PathVariable String symbol, 
                                     Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Ticker ticker = tradingService.getMarketData(user, exchange, symbol);
            
            Map<String, Object> tickerData = Map.of(
                "symbol", symbol,
                "exchange", exchange,
                "last", ticker.getLast(),
                "bid", ticker.getBid(),
                "ask", ticker.getAsk(),
                "high", ticker.getHigh(),
                "low", ticker.getLow(),
                "volume", ticker.getVolume(),
                "timestamp", ticker.getTimestamp()
            );
            
            return ResponseEntity.ok(ApiResponse.success("Ticker retrieved successfully", tickerData));
            
        } catch (Exception e) {
            logger.error("Error retrieving ticker for {} on {}: {}", symbol, exchange, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<?> getTradeHistory(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(defaultValue = "createdAt") String sortBy,
                                           @RequestParam(defaultValue = "desc") String sortDir,
                                           @RequestParam(required = false) String exchange,
                                           Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        
        try {
            List<Trade> trades;
            
            if (exchange != null && !exchange.trim().isEmpty()) {
                trades = tradingService.getUserTradesByExchange(user, exchange);
            } else {
                trades = tradingService.getUserTrades(user);
            }
            
            // Sort trades
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            
            List<Map<String, Object>> tradeResponses = trades.stream()
                    .map(this::createTradeResponse)
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Trade history retrieved successfully", tradeResponses));
            
        } catch (Exception e) {
            logger.error("Error retrieving trade history for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve trade history"));
        }
    }
    
    @GetMapping("/order/{tradeId}")
    public ResponseEntity<?> getOrderStatus(@PathVariable Long tradeId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            Optional<Trade> tradeOpt = tradingService.getTradeById(user, tradeId);
            
            if (!tradeOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Trade not found"));
            }
            
            Trade trade = tradeOpt.get();
            
            return ResponseEntity.ok(ApiResponse.success("Order status retrieved successfully", createTradeResponse(trade)));
            
        } catch (Exception e) {
            logger.error("Error retrieving order status for trade {}: {}", tradeId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve order status"));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getTradingStats(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        try {
            List<Trade> allTrades = tradingService.getUserTrades(user);
            
            long totalTrades = allTrades.size();
            long successfulTrades = allTrades.stream()
                    .mapToLong(trade -> trade.isFilled() ? 1 : 0)
                    .sum();
            long failedTrades = allTrades.stream()
                    .mapToLong(trade -> trade.isFailed() ? 1 : 0)
                    .sum();
            long pendingTrades = allTrades.stream()
                    .mapToLong(trade -> trade.isPending() ? 1 : 0)
                    .sum();
            
            BigDecimal totalVolume = allTrades.stream()
                    .filter(Trade::isFilled)
                    .map(trade -> trade.getTotalCost() != null ? trade.getTotalCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Map<String, Object> stats = Map.of(
                "totalTrades", totalTrades,
                "successfulTrades", successfulTrades,
                "failedTrades", failedTrades,
                "pendingTrades", pendingTrades,
                "totalVolume", totalVolume,
                "successRate", totalTrades > 0 ? (double) successfulTrades / totalTrades * 100 : 0.0
            );
            
            return ResponseEntity.ok(ApiResponse.success("Trading statistics retrieved successfully", stats));
            
        } catch (Exception e) {
            logger.error("Error retrieving trading stats for user {}: {}", user.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve trading statistics"));
        }
    }
    
    private Map<String, Object> createTradeResponse(Trade trade) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", trade.getId());
        response.put("exchange", trade.getExchange());
        response.put("symbol", trade.getSymbol());
        response.put("orderType", trade.getOrderType());
        response.put("side", trade.getSide());
        response.put("amount", trade.getAmount());
        response.put("price", trade.getPrice() != null ? trade.getPrice() : BigDecimal.ZERO);
        response.put("filledAmount", trade.getFilledAmount() != null ? trade.getFilledAmount() : BigDecimal.ZERO);
        response.put("averagePrice", trade.getAveragePrice() != null ? trade.getAveragePrice() : BigDecimal.ZERO);
        response.put("totalCost", trade.getTotalCost() != null ? trade.getTotalCost() : BigDecimal.ZERO);
        response.put("feeAmount", trade.getFeeAmount() != null ? trade.getFeeAmount() : BigDecimal.ZERO);
        response.put("feeCurrency", trade.getFeeCurrency());
        response.put("status", trade.getStatus());
        response.put("exchangeOrderId", trade.getExchangeOrderId());
        response.put("errorMessage", trade.getErrorMessage());
        response.put("createdAt", trade.getCreatedAt());
        response.put("updatedAt", trade.getUpdatedAt());
        response.put("executedAt", trade.getExecutedAt());
        response.put("cancelledAt", trade.getCancelledAt());
        return response;
    }
}
