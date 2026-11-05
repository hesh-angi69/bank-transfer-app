package com.example.bank_transfer_app.config;

import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository repo;

       @Override
    public void run(String... args) {
        if (repo.count() == 0) {
            repo.save(new Customer("100001", "Kamal Perera", "kamal@mail.com", new BigDecimal("50000.00")));
            repo.save(new Customer("100002", "Nimali Silva", "nimali@mail.com", new BigDecimal("25000.00")));
            repo.save(new Customer("100003", "Saman Kumara", "saman@mail.com", new BigDecimal("10000.00")));
        }
        
        if (repo.countAccounts("999999") == 0) {
            repo.save(new Customer("999999", "Bank Fee Account", "fees@bank.com", BigDecimal.ZERO));
        }
    }
}