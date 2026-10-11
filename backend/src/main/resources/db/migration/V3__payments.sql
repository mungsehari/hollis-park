-- Razorpay payments. Orders placed while payments are on start as PENDING_PAYMENT.
alter table orders add column payment_status      varchar(20) not null default 'NONE';
alter table orders add column razorpay_order_id   varchar(60);
alter table orders add column razorpay_payment_id varchar(60);
alter table orders add column paid_at             timestamptz;
create unique index orders_razorpay_order_idx on orders (razorpay_order_id) where razorpay_order_id is not null;
