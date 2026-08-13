package com.bank.banking_api.aspect;


import com.bank.banking_api.annotation.Auditable;
import com.bank.banking_api.domain.*;
import com.bank.banking_api.security.CustomUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // ThreadLocal to hold before-state data for the current request
    private static final ThreadLocal<Map<String, Object>> beforeState = new ThreadLocal<>();

    private static final ThreadLocal<String> currentAction = new ThreadLocal<>();

    public AuditAspect(AccountRepository accountRepository, AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        //1. Store the action name
        Object[] args = joinPoint.getArgs();
        String action = auditable.action();  // "Transfer", "Deposit", "Withdraw"

        try {
            //1 Dynamically extract affected accountIDs
            List<String> accountIds = resolveAccountsIds(auditable, args);

            //2. Capture Before state for these accounts
            Map<String, Object> before = new HashMap<>();
            for (String accountId : accountIds) {
                accountRepository.findByAccountNumber(accountId).ifPresent(account -> {
                    before.put("account_" + accountId, accountToJson(account));
                });
            }
            beforeState.set(before);
            currentAction.set(action);


            //2 Extract method arguments (fromaccountId,toaccountId,amount)
//        Object[] args = joinPoint.getArgs();
//        String fromAccountId = (String) args[0];
//        String toAccountId = (String) args[1];

            //3. Capture Before state(query DB for account balance)
//        Map<String, Object> before = new HashMap<>();
//        accountRepository.findByAccountNumber(fromAccountId).ifPresent(acc ->
//                before.put("from_Account_" + fromAccountId, accountToJson(acc))
//        );
//
//        accountRepository.findByAccountNumber(toAccountId).ifPresent(acc ->
//                before.put("to_account" + toAccountId, accountToJson(acc))
//        );
//        beforeState.set(before);


            //4 Execute the actual method (the transfer)
            Object result = joinPoint.proceed();

            //5. If we reach here, the method succeeded
            //Capture After state (query DB again)
            Map<String, Object> after = new HashMap<>();
//        accountRepository.findByAccountNumber(fromAccountId).ifPresent(acc->
//                after.put("from_Account_" + fromAccountId, accountToJson(acc))
//        );
//        accountRepository.findByAccountNumber(toAccountId).ifPresent(acc->
//                after.put("to_account" + toAccountId, accountToJson(acc))
//        );


            for (String accountId : accountIds) {
                accountRepository.findByAccountNumber(accountId).ifPresent(account -> {
                    after.put("account_" + accountId, accountToJson(account));
                });
            }

            //6 Build the details Json
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("before", before);
            detailsMap.put("after", after);
            String detailsJson = objectMapper.writeValueAsString(detailsMap);

            //7 Extract transaction Id from the result
            UUID transactionId = null;
            if (result instanceof Transaction tx) {
                transactionId = tx.getId();
            }

            //8 Extract actor Id from securityContext
            UUID actorId = getCurrentId();

            //9 Create the audit log
            AuditLog auditLog = AuditLog.builder()
                    .withId(UUID.randomUUID())
                    .withTransactionId(transactionId)
                    .withActorId(actorId)
                    .withActionName(action)
                    .withDetails(detailsJson)
                    .withTimestamp(Instant.now())
                    .build();

            //10. Write the audit log only after the transaction commit
            registerAuditLogOnCommit(auditLog);

            //11. Clean up ThreadLocal
            return result;
        } finally {
            //Before point out the mistake I'm done this without try-catch, but after i understand that the
            //line 81 joint.proceed throw error the threadlocal didn't clean up which create memory leak
            beforeState.remove();
            currentAction.remove();
        }
    }

    private void registerAuditLogOnCommit(AuditLog auditLog) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            auditLogRepository.save(auditLog);
                        }
                    }
            );
        } else {
            //Fallback: if no transaction is active, save immediately
            auditLogRepository.save(auditLog);
        }
    }


    private UUID getCurrentId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    private List<String> resolveAccountsIds(Auditable auditable, Object[] args) {
        return switch (auditable.action()) {
            case "TRANSFER" -> {
                // Method signature
                int sourceIdx=auditable.sourceAccountArgIndex();
                int targetIdx=auditable.targetAccountArgIndex();

                if(sourceIdx>=0 && targetIdx>=0 && args.length>Math.max(sourceIdx,targetIdx)) {
                    yield List.of((String) args[sourceIdx], (String) args[targetIdx]);
                }
                throw new IllegalArgumentException("Transfer requires at least 2 account IDs");
            }
            case "DEPOSIT" -> {
                if (auditable.sourceAccountArgIndex() >= 0) {
                    yield List.of((String) args[auditable.sourceAccountArgIndex()]);
                }
                throw new IllegalArgumentException("Deposit requires at least 1 account ID");
            }
            case "WITHDRAW" -> {
                if (auditable.sourceAccountArgIndex() >= 0) {
                    yield List.of((String) args[auditable.sourceAccountArgIndex()]);
                }
                throw new IllegalArgumentException("Withdrawal requires at least 1 account ID");
            }
            case "CLOSE_ACCOUNT", "FREEZE_ACCOUNT", "UNFREEZE_ACCOUNT" -> {
                if (args.length >= 1) {
                    yield List.of((String) args[0]);
                }
                throw new IllegalArgumentException("Account status change requires at least 1 account ID");
            }
            default -> throw new IllegalArgumentException("Unknown action" + auditable.action());
        };
    }

    private String accountToJson(Account account) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("accountNumber", account.getAccountNumber());
            map.put("balance", account.getBalance());
            map.put("currency", account.getBalance().getCurrency().getCurrencyCode());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\":\"serialization failure\"}";
        }
    }
}