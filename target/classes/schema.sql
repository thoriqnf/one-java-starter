CREATE TABLE IF NOT EXISTS account (
    account_number VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS transaction_history (
    id SERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    source_account VARCHAR(20) NOT NULL,
    target_account VARCHAR(20),
    amount NUMERIC(15,2) NOT NULL,
    fee NUMERIC(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
