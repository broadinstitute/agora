create table ENTITIES (ALIAS VARCHAR(255) NOT NULL,ID INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY);
create unique index idx_entitys on ENTITIES (ALIAS);
create table USERS (EMAIL VARCHAR(255) NOT NULL,ID INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY);
create unique index idx_users on USERS (EMAIL);
create table PERMISSIONS (USER_ID INTEGER NOT NULL,ENTITY_ID INTEGER NOT NULL,ROLE INTEGER NOT NULL);
alter table PERMISSIONS add constraint pk_permissions primary key(USER_ID,ENTITY_ID);
alter table PERMISSIONS add constraint ENTITY_FK foreign key(ENTITY_ID) references ENTITIES(ID) on update NO ACTION on delete NO ACTION;
alter table PERMISSIONS add constraint USER_FK foreign key(USER_ID) references USERS(ID) on update NO ACTION on delete NO ACTION;