alter table issue drop index idx_issue_reference;

alter table issue add description text null;
alter table issue add effort varchar(20) null;
