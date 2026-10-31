package com.larkspur.inventory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

interface StockRepository extends JpaRepository<StockItem, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from StockItem s where s.sku = :sku")
  Optional<StockItem> findForUpdate(@Param("sku") String sku);
}
