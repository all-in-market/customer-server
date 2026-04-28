ALTER TABLE payments ADD COLUMN merchant_uid character varying(255) NOT NULL;
ALTER TABLE payments ADD CONSTRAINT payments_merchant_uid_key UNIQUE (merchant_uid);
