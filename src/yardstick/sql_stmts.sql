create table job (
	id int auto_increment primary key,
	tenant_id int not null references tenant(id),
	name varchar(64) not null,
    params text null,
	status varchar(16) not null default 'pending',
	queued_at timestamp not null default CURRENT_TIMESTAMP,
	started_at timestamp null,
	completed_at timestamp null,
  completition_report text null
);