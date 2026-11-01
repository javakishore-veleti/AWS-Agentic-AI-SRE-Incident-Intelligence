package com.larkspur.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductRepository extends JpaRepository<Product, UUID> {
  List<Product> findTop100ByOrderByCreatedAtDesc();

  @Query("""
      select p from Product p
      where (:name is null or lower(p.name) like lower(concat('%', :name, '%')))
        and (:sku is null or lower(p.sku) like lower(concat('%', :sku, '%')))
        and (:category is null or lower(p.category) like lower(concat('%', :category, '%')))
        and (:status is null or p.status = :status)
      order by p.createdAt desc
      """)
  List<Product> search(
      @Param("name") String name,
      @Param("sku") String sku,
      @Param("category") String category,
      @Param("status") ProductStatus status,
      Pageable pageable);

  List<Product> findByIdIn(List<UUID> ids);
}
