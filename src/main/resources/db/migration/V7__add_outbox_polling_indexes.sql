CREATE INDEX idx_dashboard_outbox_polling ON dashboard_outbox (processed, retry_count, id);
CREATE INDEX idx_history_outboxes_polling ON history_outboxes (processed, retry_count, id);
