package com.example.bank_transfer_app.service;

import java.math.RoundingMode;
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
@Transactional(readOnly = true)   
public class BankService {

    private final CustomerRepository customerRepo;
    private final BankTransactionRepository txRepo;

  

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
        return total == null ? BigDecimal.ZERO : total;   
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

    //------

        public List<BankTransaction> getLargeTransfers(BigDecimal min) {
        return txRepo.findLargeTransfers(min);
    }

   

    @Transactional  
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

   
     
        @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String from, String to,
                                    BigDecimal amount, boolean simulateFailure) {

        
        if (from.equals(to)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        if (customerRepo.countAccounts(from) == 0) {
            throw new AccountNotFoundException("Account " + from + " not found");
        }
        if (customerRepo.countAccounts(to) == 0) {
            throw new AccountNotFoundException("Account " + to + " not found");
        }

        

        
        BigDecimal fee = amount.multiply(new BigDecimal("0.01"))
                               .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);

      
        int rows = customerRepo.debit(from, totalDebit);
        if (rows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + from);
        }

        
        if (simulateFailure) {
            throw new BankException("Simulated system failure after the withdrawal");
        }

        
        customerRepo.credit(to, amount);
        customerRepo.credit("999999", fee);

        
        return txRepo.save(new BankTransaction(from, to, amount,
                TransactionStatus.SUCCESS, "Transfer completed (fee " + fee + ")"));
    }

    //-----
        public BigDecimal getTotalSent(String account) {
        return txRepo.sumSentByAccount(account, TransactionStatus.SUCCESS);
    }
    

        @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String account, BigDecimal amount) {
        if (customerRepo.countAccounts(account) == 0) {
            throw new AccountNotFoundException("Account " + account + " not found");
        }
        customerRepo.credit(account, amount);
        return txRepo.save(new BankTransaction(null, account, amount,
                TransactionStatus.SUCCESS, "Deposit"));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String account, BigDecimal amount) {
        if (customerRepo.countAccounts(account) == 0) {
            throw new AccountNotFoundException("Account " + account + " not found");
        }
        int rows = customerRepo.debit(account, amount);
        if (rows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + account);
        }
        return txRepo.save(new BankTransaction(account, null, amount,
                TransactionStatus.SUCCESS, "Withdrawal"));
    }
}