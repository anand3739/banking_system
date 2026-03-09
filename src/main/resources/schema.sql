CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_customers FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    sender_account_id BIGINT,
    receiver_account_id BIGINT,
    amount NUMERIC(19,2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    transaction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS fraud_alerts (
    alert_id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    fraud_reason VARCHAR(255) NOT NULL,
    risk_score INTEGER NOT NULL,
    flagged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fraud_alerts_transactions FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_tx_sender_time ON transactions(sender_account_id, transaction_time);
CREATE INDEX IF NOT EXISTS idx_tx_receiver_time ON transactions(receiver_account_id, transaction_time);
CREATE INDEX IF NOT EXISTS idx_fraud_alerts_transaction_id ON fraud_alerts(transaction_id);
