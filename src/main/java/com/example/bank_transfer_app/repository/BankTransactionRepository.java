package com.example.bank_transfer_app.repository;

import com.example.bank_transfer_app.model.BankTransaction;
import com.example.bank_transfer_app.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.math.BigDecimal;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    
    @Query("SELECT t FROM BankTransaction t ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findAllLatest();

    
    @Query("SELECT t FROM BankTransaction t " +
           "WHERE t.fromAccount = :account OR t.toAccount = :account " +
           "ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findHistory(@Param("account") String account);

   
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status " +
           "ORDER BY t.createdAt DESC, t.id DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

        
    @Query("SELECT t FROM BankTransaction t WHERE t.amount > :min " +
           "ORDER BY t.amount DESC, t.id DESC")
    List<BankTransaction> findLargeTransfers(@Param("min") BigDecimal min);

    //----
        
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM BankTransaction t " +
           "WHERE t.fromAccount = :account AND t.status = :status")
    BigDecimal sumSentByAccount(@Param("account") String account,
                                @Param("status") TransactionStatus status);
}