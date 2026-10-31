package com.larkspur.offers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OfferRepository extends JpaRepository<Offer, UUID> {
  List<Offer> findTop100ByOrderByCreatedAtDesc();
}
