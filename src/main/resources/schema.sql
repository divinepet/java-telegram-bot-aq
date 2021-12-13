create table if not exists bot_qa_users (
    id varchar (80) not null,
    username varchar (80),
    firstname varchar(80) not null,
    lastname varchar(80),
    blocked text[]
);

create table if not exists bot_qa_state
(
    updid varchar(80),
    sender_id varchar(80),
    receiver_id varchar(80),
    wait_answer bool default false,
    edit_msg_id bigint,
    edit_msg varchar(1000)
);