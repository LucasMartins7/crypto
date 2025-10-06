package com.cryptotrader.repository;

import com.cryptotrader.entity.Trade;
import com.cryptotrader.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    
    List<Trade> findByUser(User user);
    
    Page<Trade> findByUser(User user, Pageable pageable);
    
    List<Trade> findByUserAndStatus(User user, String status);
    
    List<Trade> findByUserAndExchange(User user, String exchange);
    
    List<Trade> findByUserAndSymbol(User user, String symbol);
    
    Optional<Trade> findByIdAndUser(Long id, User user);
    
    Optional<Trade> findByExchangeOrderIdAndUser(String exchangeOrderId, User user);
    
    @Query("SELECT t FROM Trade t WHERE t.user = :user AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Trade> findByUserAndDateRange(@Param("user") User user, 
                                     @Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trade t WHERE t.user = :user AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Trade> findByUserAndDateRange(@Param("user") User user, 
                                     @Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);
    
    @Query("SELECT t FROM Trade t WHERE t.status = 'PENDING' AND t.createdAt < :cutoffTime")
    List<Trade> findStuckPendingTrades(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT SUM(t.totalCost) FROM Trade t WHERE t.user = :user AND t.status = 'FILLED' AND t.side = 'BUY'")
    BigDecimal getTotalBuyVolumeByUser(@Param("user") User user);
    
    @Query("SELECT SUM(t.totalCost) FROM Trade t WHERE t.user = :user AND t.status = 'FILLED' AND t.side = 'SELL'")
    BigDecimal getTotalSellVolumeByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.user = :user AND t.status = 'FILLED'")
    Long countSuccessfulTradesByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.user = :user AND t.status = 'FAILED'")
    Long countFailedTradesByUser(@Param("user") User user);
    
    @Query("SELECT t.symbol, COUNT(t) FROM Trade t WHERE t.user = :user AND t.status = 'FILLED' GROUP BY t.symbol ORDER BY COUNT(t) DESC")
    List<Object[]> getMostTradedSymbolsByUser(@Param("user") User user);
    
    @Query("SELECT t.exchange, COUNT(t) FROM Trade t WHERE t.user = :user GROUP BY t.exchange")
    List<Object[]> getTradeCountByExchangeForUser(@Param("user") User user);
    
    @Query("SELECT DATE(t.createdAt), COUNT(t) FROM Trade t WHERE t.user = :user AND t.createdAt >= :startDate GROUP BY DATE(t.createdAt) ORDER BY DATE(t.createdAt)")
    List<Object[]> getDailyTradeCountByUser(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT SUM(t.feeAmount) FROM Trade t WHERE t.user = :user AND t.status = 'FILLED' AND t.feeCurrency = :currency")
    BigDecimal getTotalFeesByUserAndCurrency(@Param("user") User user, @Param("currency") String currency);
}
