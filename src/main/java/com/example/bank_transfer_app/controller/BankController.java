package com.example.bank_transfer_app.controller;

import com.example.bank_transfer_app.dto.TransferRequest;
import com.example.bank_transfer_app.model.TransactionStatus;
import com.example.bank_transfer_app.service.BankService;
import com.example.bank_transfer_app.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    
    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("customers", bankService.getCustomers(keyword));
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("customerCount", bankService.getCustomerCount());
        model.addAttribute("successCount", bankService.countByStatus(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countByStatus(TransactionStatus.FAILED));
        model.addAttribute("transactions", bankService.getLatestTransactions());
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        loadDashboard(model, keyword);
        model.addAttribute("transferRequest", new TransferRequest());
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@RequestParam String accountNumber,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam BigDecimal balance,
                              RedirectAttributes ra) {
        try {
            bankService.createCustomer(accountNumber, fullName, email, balance);
            ra.addFlashAttribute("success", "Customer " + fullName + " added");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest req,
                           BindingResult result, Model model, RedirectAttributes ra) {

        
        if (result.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }

        try {
            bankService.transfer(req.getFromAccount(), req.getToAccount(),
                                 req.getAmount(), req.isSimulateFailure());
            ra.addFlashAttribute("success", "Transfer of " + req.getAmount() + " completed");
        } catch (Exception e) {
            
            auditService.recordFailure(req.getFromAccount(), req.getToAccount(),
                                       req.getAmount(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model,
                          RedirectAttributes ra) {
        try {
            model.addAttribute("customer", bankService.getCustomer(accountNumber));
            model.addAttribute("transactions", bankService.getHistory(accountNumber));

            //------
                model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));


            return "history";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    //----
        @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min,
                                 Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transactions", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

        @PostMapping("/deposit")
    public String deposit(@RequestParam String account,
                          @RequestParam BigDecimal amount,
                          RedirectAttributes ra) {
        try {
            bankService.deposit(account, amount);
            ra.addFlashAttribute("success", "Deposited " + amount + " to " + account);
        } catch (Exception e) {
            auditService.recordFailure(null, account, amount, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String account,
                           @RequestParam BigDecimal amount,
                           RedirectAttributes ra) {
        try {
            bankService.withdraw(account, amount);
            ra.addFlashAttribute("success", "Withdrew " + amount + " from " + account);
        } catch (Exception e) {
            auditService.recordFailure(account, null, amount, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage() + ". All changes were rolled back.");
        }
        return "redirect:/";
    }
}