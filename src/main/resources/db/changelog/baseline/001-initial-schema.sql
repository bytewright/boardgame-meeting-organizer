  alter table if exists games 
       drop constraint if exists FKo59wtadyo5hgfk4vgggpjilfq;

    alter table if exists meetup_joins 
       drop constraint if exists FKkg7xc4eqhb43jxu3rgjme4cvf;

    alter table if exists meetup_joins 
       drop constraint if exists FKdv7f4hcu3ix95v7unq122puiq;

    alter table if exists meetup_offered_games 
       drop constraint if exists FKav4cw0ynymhtfyfq6cdxi5vl9;

    alter table if exists meetup_offered_games 
       drop constraint if exists FKssykgj46bkw4e2bp858wprpx3;

    alter table if exists meetups 
       drop constraint if exists FKn27wha3j17v0yiripaoc1me2k;

    alter table if exists user_contact_infos 
       drop constraint if exists FKp95x5f9uq6yvapwsn643lmkie;

    drop table if exists games cascade;

    drop table if exists meetup_joins cascade;

    drop table if exists meetup_offered_games cascade;

    drop table if exists meetups cascade;

    drop table if exists registered_users cascade;

    drop table if exists user_contact_infos cascade;

    create table games (
        complexity float(53),
        maxPlayers integer not null,
        minPlayers integer not null,
        optimalPlayers integer,
        playTimeMinutesPerPlayer integer not null,
        bggId bigint,
        created_at timestamp(6) with time zone not null,
        deleted_at timestamp(6) with time zone,
        modified_at timestamp(6) with time zone,
        id uuid not null,
        owner_id uuid not null,
        name varchar(1024) not null,
        artworkLink varchar(255),
        description TEXT,
        urls varchar(255),
        primary key (id)
    );

    create table meetup_joins (
        created_at timestamp(6) with time zone not null,
        anonToken uuid,
        id uuid not null,
        meetup_id uuid not null,
        user_id uuid,
        contactInfo varchar(255),
        displayName varchar(255) not null,
        requestState varchar(255) not null check (requestState in ('OPEN','ACCEPTED','DECLINED','CANCELED')),
        primary key (id)
    );

    create table meetup_offered_games (
        game_id uuid not null,
        meeting_id uuid not null
    );

    create table meetups (
        allowAnonSignup boolean not null,
        canceled boolean not null,
        durationHours integer not null,
        joinSlots integer not null,
        unlimitedSlots boolean not null,
        created_at timestamp(6) with time zone not null,
        eventDate timestamp(6) with time zone not null,
        registrationClosing timestamp(6) with time zone not null,
        modified_at timestamp(6) with time zone,
        creator_id uuid not null,
        id uuid not null,
        description varchar(255),
        title varchar(255) not null,
        primary key (id)
    );

    create table adapter_settings (
        adapterSettings jsonb not null,
        created_at timestamp(6) with time zone not null,
        modified_at timestamp(6) with time zone,
        id uuid not null,
        adapterName varchar(1024) not null,
        primary key (id),
        constraint UC_ADAPTER_SETTING_NAME unique (adapterName)
    );

    create table registered_users (
        created_at timestamp(6) with time zone not null,
        last_login timestamp(6) with time zone,
        modified_at timestamp(6) with time zone,
        id uuid not null,
        primaryContactId uuid,
        displayName varchar(1024) not null,
        loginName varchar(255) not null,
        passwordHash varchar(255) not null,
        preferredLocale varchar(255),
        role varchar(255) not null check (role in ('USER','ADMIN')),
        status varchar(255) not null check (status in ('PENDING_APPROVAL','ACTIVE','BANNED')),
        primary key (id),
        constraint UC_USER_LOGIN_NAME unique (loginName)
    );

    create table user_contact_infos (
        id uuid not null,
        user_id uuid not null,
        jsonData text not null,
        type varchar(255) check (type in ('EMAIL','TELEGRAM','SIGNAL','ADDRESS','PHONE')),
        primary key (id)
    );

    alter table if exists games 
       add constraint FKo59wtadyo5hgfk4vgggpjilfq 
       foreign key (owner_id) 
       references registered_users;

    alter table if exists meetup_joins 
       add constraint FKkg7xc4eqhb43jxu3rgjme4cvf 
       foreign key (meetup_id) 
       references meetups;

    alter table if exists meetup_joins 
       add constraint FKdv7f4hcu3ix95v7unq122puiq 
       foreign key (user_id) 
       references registered_users;

    alter table if exists meetup_offered_games 
       add constraint FKav4cw0ynymhtfyfq6cdxi5vl9 
       foreign key (meeting_id) 
       references games;

    alter table if exists meetup_offered_games 
       add constraint FKssykgj46bkw4e2bp858wprpx3 
       foreign key (game_id) 
       references meetups;

    alter table if exists meetups 
       add constraint FKn27wha3j17v0yiripaoc1me2k 
       foreign key (creator_id) 
       references registered_users;

    alter table if exists user_contact_infos 
       add constraint FKp95x5f9uq6yvapwsn643lmkie 
       foreign key (user_id) 
       references registered_users