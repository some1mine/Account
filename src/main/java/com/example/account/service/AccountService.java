package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     *
     * 사용자가 있는지 조회
     * 계좌의 번호를 생성하고
     * 계좌를 저장하고, 그 정보를 넘긴다.
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        validateCreateAccount(accountUser);

        String newAccountNUmber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Long.parseLong(account.getAccountNumber()) + 1) + "").orElse("10000000000");

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                    .accountUser(accountUser).accountStatus(AccountStatus.IN_USE)
                    .accountNumber(newAccountNUmber)
                    .balance(initialBalance)
                    .registeredAt(LocalDateTime.now()).build()));
    }

    private AccountUser getAccountUser(Long userId) {
        return accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
    }

    private void validateCreateAccount(AccountUser accountUser) {
        if (accountRepository.countByAccountUser(accountUser) >= 10)
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
    }

    @Transactional
    public Account getAccount(Long id) {
        if(id < 0) throw new RuntimeException("Minus");
        return accountRepository.findById(id).get();
    }
    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);
        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream().map(AccountDto::fromEntity).collect(Collectors.toList());
    }
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }
    @Transactional
    public void validateDeleteAccount(AccountUser accountUser, Account account) {
        if(accountUser.getId() != account.getAccountUser().getId()) throw new AccountException(USER_ACCOUNT_UN_MATCH);

        if(account.getAccountStatus() == AccountStatus.UNREGISTERED) throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);

        if (account.getBalance() > 0) throw new AccountException(BALANCE_NOT_EMPTY);
    }

}