CREATE UNIQUE INDEX uq_payments_order_success
    ON payments (order_id)
    WHERE status = 'SUCCESS';