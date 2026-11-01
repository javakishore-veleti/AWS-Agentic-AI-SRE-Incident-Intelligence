package com.larkspur.offers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OfferRepository extends JpaRepository<Offer, UUID> {
  List<Offer> findTop100ByOrderByCreatedAtDesc();

  @Query("""
      select o from Offer o
      where (:name is null or lower(o.name) like lower(concat('%', :name, '%')))
        and (:code is null or lower(o.code) like lower(concat('%', :code, '%')))
        and (:status is null or o.status = :status)
      order by o.createdAt desc
      """)
  List<Offer> search(
      @Param("name") String name,
      @Param("code") String code,
      @Param("status") OfferStatus status,
      Pageable pageable);
}
