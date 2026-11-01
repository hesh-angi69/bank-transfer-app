package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.exception.AccountNotFoundException;
import com.example.bank_transfer_app.exception.BankException;
import com.example.bank_transfer_app.exception.InsufficientBalanceException;
import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.Customer;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import com.example.bank_transfer_app.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // default: හැම method එකක්ම read-only
public class BankService {

    private final CustomerRepository customerRepo;
    private final BankTransactionRepository txRepo;

    // ---------- Read-only methods ----------

    public List<Customer> getCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepo.findAllOrdered();
        }
        return customerRepo.searchByName(keyword.trim());
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepo.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account " + accountNumber + " not found"));
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepo.getTotalBalance();
        return total == null ? BigDecimal.ZERO : total;   // customers නැත්නම් null
    }

    public long getCustomerCount() {
        return customerRepo.countCustomersNative();
    }

    public List<BankTransaction> getLatestTransactions() {
        return txRepo.findAllLatest().stream().limit(10).toList();
    }

    public List<BankTransaction> getHistory(String accountNumber) {
        return txRepo.findHistory(accountNumber);
    }

    public long countByStatus(TransactionStatus status) {
        return txRepo.countByStatus(status);
    }

    // ---------- Write methods ----------

    @Transactional   // read-write
    public Customer createCustomer(String accountNumber, String fullName,
                                   String email, BigDecimal balance) {
        if (customerRepo.countAccounts(accountNumber) > 0) {
            throw new BankException("Account number " + accountNumber + " already exists");
        }
        if (customerRepo.countEmails(email) > 0) {
            throw new BankException("Email " + email + " already exists");
        }
        return customerRepo.save(new Customer(accountNumber, fullName, email, balance));
    }

    /**
     * Lab එකේ හදවත: සියලු steps එකම transaction එකක්.
     * ඕනෑම exception එකක් ආවොත් ඔක්කොම rollback වෙනවා.
     */
    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String from, String to,
                                    BigDecimal amount, boolean simulateFailure) {

        // Step 1: Validate
        if (from.equals(to)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        if (customerRepo.countAccounts(from) == 0) {
            throw new AccountNotFoundException("Account " + from + " not found");
        }
        if (customerRepo.countAccounts(to) == 0) {
            throw new AccountNotFoundException("Account " + to + " not found");
        }

        // Step 2: Withdraw (balance මදි නම් 0 rows)
        int rows = customerRepo.debit(from, amount);
        if (rows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + from);
        }

        // Step 3: Experiment: withdraw කරලා ඉවර වුණාට පස්සේ crash එකක්
        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        // Step 4: Deposit
        customerRepo.credit(to, amount);

        // Step 5: SUCCESS record එක save කරනවා
        return txRepo.save(new BankTransaction(from, to, amount,
                TransactionStatus.SUCCESS, "Transfer completed"));
    }
}