-- Unique gateway references for Paystack (and future gateways)
ALTER TABLE payments ADD CONSTRAINT uk_payments_gateway_reference UNIQUE (gateway_reference);
