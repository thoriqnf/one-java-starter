public interface AccountRepository {
    Account findByAccountNumber(String accountNumber);
    void update(Account account);
}
