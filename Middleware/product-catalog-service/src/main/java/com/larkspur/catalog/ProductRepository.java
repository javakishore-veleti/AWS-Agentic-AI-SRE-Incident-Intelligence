package com.larkspur.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {
  List<Product> findTop100ByOrderByCreatedAtDesc();

  List<Product> findByIdIn(List<UUID> ids);
}
