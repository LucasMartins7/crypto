package com.cryptotrader.repository;

import com.cryptotrader.entity.ApiKey;
import com.cryptotrader.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    List<ApiKey> findByUser(User user);
    
    List<ApiKey> findByUserAndIsActive(User user, Boolean isActive);
    
    Optional<ApiKey> findByUserAndExchange(User user, String exchange);
    
    Optional<ApiKey> findByIdAndUser(Long id, User user);
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.user = :user AND ak.exchange = :exchange AND ak.isActive = true")
    Optional<ApiKey> findActiveApiKeyByUserAndExchange(@Param("user") User user, @Param("exchange") String exchange);
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true")
    List<ApiKey> findAllActiveApiKeys();
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.lastUsed IS NULL OR ak.lastUsed < :cutoffDate")
    List<ApiKey> findUnusedApiKeys(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.testConnectionStatus = 'FAILED'")
    List<ApiKey> findFailedConnectionApiKeys();
    
    @Query("SELECT COUNT(ak) FROM ApiKey ak WHERE ak.user = :user AND ak.isActive = true")
    Long countActiveApiKeysByUser(@Param("user") User user);
    
    @Query("SELECT ak.exchange, COUNT(ak) FROM ApiKey ak WHERE ak.isActive = true GROUP BY ak.exchange")
    List<Object[]> countApiKeysByExchange();
    
    boolean existsByUserAndExchange(User user, String exchange);
}
