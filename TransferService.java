public class TransferService {
    private final AccountRepository repository;

    public TransferService(AccountRepository repository) {
        this.repository = repository;
    }

    public void transfer(String from, String to, double amount) {
        Account source = repository.findByAccountNumber(from);
        Account target = repository.findByAccountNumber(to);
        
        if (source == null || target == null) {
            throw new IllegalArgumentException("Account not found");
        }

        source.debit(amount);
        target.credit(amount);
        
        repository.update(source);
        repository.update(target);
    }
}
