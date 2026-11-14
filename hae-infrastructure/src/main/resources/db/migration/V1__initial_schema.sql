-- Schema inicial do sistema de HAEs (remodelado na refatoração de 2026-11).

create table institution (
    id                 uuid primary key,
    code               integer      not null unique check (code > 0),
    name               varchar(150) not null,
    address            varchar(255) not null,
    weekly_hours_limit integer      not null check (weekly_hours_limit >= 0),
    active             boolean      not null default true,
    created_at         timestamptz  not null,
    updated_at         timestamptz  not null,
    version            bigint       not null default 0
);
create unique index ux_institution_name on institution (lower(name));

create table course (
    id             uuid primary key,
    institution_id uuid         not null references institution (id) on delete cascade,
    name           varchar(120) not null,
    created_at     timestamptz  not null,
    updated_at     timestamptz  not null,
    version        bigint       not null default 0
);
create unique index ux_course_institution_name on course (institution_id, lower(name));

create table employee (
    id             uuid primary key,
    name           varchar(100) not null,
    email          varchar(254) not null unique,
    password_hash  varchar(100) not null,
    role           varchar(20)  not null check (role in ('PROFESSOR', 'COORDENADOR', 'DIRETOR', 'ADMIN', 'DEV')),
    institution_id uuid references institution (id),
    course_id      uuid references course (id),
    created_at     timestamptz  not null,
    updated_at     timestamptz  not null,
    version        bigint       not null default 0,
    constraint ck_employee_institution check (role in ('ADMIN', 'DEV') or institution_id is not null)
);
create index ix_employee_institution on employee (institution_id);
create index ix_employee_course_role on employee (course_id, role);
create index ix_employee_name on employee (lower(name));

create table hae (
    id             uuid primary key,
    employee_id    uuid          not null references employee (id),
    institution_id uuid          not null references institution (id),
    course_id      uuid          not null references course (id),
    title          varchar(200)  not null,
    description    text          not null,
    observations   text,
    type           varchar(20)   not null,
    modality       varchar(20)   not null,
    dimension      varchar(80)   not null,
    weekly_hours   integer       not null check (weekly_hours between 1 and 40),
    start_date     date          not null,
    end_date       date          not null,
    status         varchar(30)   not null,
    reviewer_id    uuid references employee (id) on delete set null,
    review_comment varchar(1000),
    viewed         boolean       not null default false,
    created_at     timestamptz   not null,
    updated_at     timestamptz   not null,
    version        bigint        not null default 0,
    constraint ck_hae_period check (end_date > start_date)
);
create index ix_hae_institution_start on hae (institution_id, start_date);
create index ix_hae_employee on hae (employee_id);
create index ix_hae_course on hae (course_id);
create index ix_hae_status on hae (status);

create table hae_schedule (
    hae_id      uuid        not null references hae (id) on delete cascade,
    day_of_week varchar(10) not null,
    start_time  time        not null,
    end_time    time        not null,
    primary key (hae_id, day_of_week),
    constraint ck_hae_schedule_range check (end_time > start_time)
);

create table hae_student (
    hae_id   uuid        not null references hae (id) on delete cascade,
    position integer     not null,
    ra       varchar(20) not null,
    primary key (hae_id, position)
);

create table hae_attachment (
    hae_id       uuid         not null references hae (id) on delete cascade,
    id           uuid         not null,
    file_name    varchar(120) not null,
    content_type varchar(50)  not null,
    size_bytes   bigint       not null,
    url          varchar(500) not null,
    storage_key  varchar(255) not null,
    uploaded_at  timestamptz  not null,
    primary key (hae_id, id)
);

create table hae_closure (
    id                   uuid primary key,
    hae_id               uuid        not null references hae (id) on delete cascade,
    position             integer     not null,
    report_type          varchar(20) not null,
    role                 varchar(100),
    student_count        integer,
    student_names        text,
    approved_students    text,
    project_info         text,
    student_info         text,
    support_type         varchar(100),
    description          text,
    certificate_students text,
    submitted_at         timestamptz not null,
    decision             varchar(10),
    reviewer_id          uuid references employee (id) on delete set null,
    review_comment       varchar(1000),
    decided_at           timestamptz
);
create index ix_hae_closure_hae on hae_closure (hae_id);

create table pending_registration (
    id             uuid primary key,
    name           varchar(100) not null,
    email          varchar(254) not null unique,
    password_hash  varchar(100) not null,
    institution_id uuid         not null references institution (id) on delete cascade,
    course_id      uuid references course (id) on delete cascade,
    token_hash     varchar(64)  not null unique,
    expires_at     timestamptz  not null
);

create table password_reset_token (
    id          uuid primary key,
    employee_id uuid        not null references employee (id) on delete cascade,
    token_hash  varchar(64) not null unique,
    expires_at  timestamptz not null
);
create index ix_password_reset_employee on password_reset_token (employee_id);
