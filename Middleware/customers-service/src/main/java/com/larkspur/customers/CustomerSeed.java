package com.larkspur.customers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class CustomerSeed implements CommandLineRunner {
  static final String DEMO_PASSWORD = "Heddle#1";
  private final CustomerRepository customers;
  private final BCryptPasswordEncoder passwords;

  CustomerSeed(CustomerRepository customers, BCryptPasswordEncoder passwords) {
    this.customers = customers;
    this.passwords = passwords;
  }

  @Override
  public void run(String... args) {
    boolean empty = customers.count() == 0;
    String hash = passwords.encode(DEMO_PASSWORD);
    if (customers.findByEmail("desk@heddle.example").isEmpty()) {
      Customer admin = new Customer();
      admin.id = UUID.fromString("30000000-0000-4000-8000-000000000099");
      admin.fullName = "Heddle Desk";
      admin.email = "desk@heddle.example";
      admin.phone = "";
      admin.status = CustomerStatus.ACTIVE;
      admin.role = AccountRole.ADMIN;
      admin.passwordHash = hash;
      customers.save(admin);
    }
    if (!empty) {
      return;
    }
    Instant now = Instant.now();
    save(1, "Amira Shah", "amira.shah@heddle.example", "415-555-0148", CustomerStatus.ACTIVE, hash, now.minus(12, ChronoUnit.DAYS));
    save(2, "Jonah Ellis", "jonah.ellis@heddle.example", "415-555-0172", CustomerStatus.ACTIVE, hash, now.minus(11, ChronoUnit.DAYS));
    save(3, "Priya Raman", "priya.raman@heddle.example", "628-555-0119", CustomerStatus.ACTIVE, hash, now.minus(9, ChronoUnit.DAYS));
    save(4, "Luis Ortega", "luis.ortega@heddle.example", "510-555-0164", CustomerStatus.SUSPENDED, hash, now.minus(7, ChronoUnit.DAYS));
    save(5, "Helen Cho", "helen.cho@heddle.example", "650-555-0188", CustomerStatus.ACTIVE, hash, now.minus(4, ChronoUnit.DAYS));
    save(6, "Marco Bianchi", "marco.bianchi@heddle.example", "707-555-0133", CustomerStatus.ACTIVE, hash, now.minus(2, ChronoUnit.DAYS));
  }

  private void save(int sequence, String name, String email, String phone, CustomerStatus status, String hash, Instant createdAt) {
    Customer customer = new Customer();
    customer.id = UUID.fromString("30000000-0000-4000-8000-0000000000" + String.format("%02d", sequence));
    customer.fullName = name;
    customer.email = email;
    customer.phone = phone;
    customer.status = status;
    customer.role = AccountRole.CUSTOMER;
    customer.passwordHash = hash;
    customer.createdAt = createdAt;
    customer.updatedAt = createdAt;
    customers.save(customer);
  }
}
