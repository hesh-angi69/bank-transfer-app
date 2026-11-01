package com.example.bank_transfer_app.service;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository txRepo;

    // REQUIRES_NEW: transfer() rollback වුණත් FAILED record එක වෙනම transaction එකක ඉතිරි වෙනවා
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String from, String to, BigDecimal amount, String reason) {
        txRepo.save(new BankTransaction(from, to, amount,
                TransactionStatus.FAILED, reason));
    }
}