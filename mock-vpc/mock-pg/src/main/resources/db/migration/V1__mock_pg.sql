create table mock_pg_payments(payment_id varchar(36) primary key,currency varchar(3) not null,
 approved_amount numeric(24,8) not null default 0 check(approved_amount>=0),
 refunded_amount numeric(24,8) not null default 0 check(refunded_amount>=0),check(refunded_amount<=approved_amount));
create table mock_pg_operations(operation_id varchar(36) primary key,payment_id varchar(36) not null references mock_pg_payments(payment_id),
 result_json text not null,business_date date not null default (now() at time zone 'Asia/Seoul')::date);
create table mock_pg_attempts(operation_id varchar(36) primary key,attempts int not null check(attempts>0));
create table mock_pg_scenario(id int primary key,next_mode varchar(40) not null);
insert into mock_pg_scenario values(1,'SUCCESS');
