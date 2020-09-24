-- uuid-ossp is required to generate uuid in ID primary key fields
-- fails when not run with DB Owner / super user privilege
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--;;
CREATE TABLE timers(
    id UUID primary key DEFAULT uuid_generate_v4(),
    user_id bigserial not null references users(id),
    task_id bigserial not null references tasks(id),
    note text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
--;;
CREATE INDEX timers_user_id_idx
ON timers(user_id);
--;;
CREATE INDEX timers_task_id_idx
ON timers(task_id);
--;;
CREATE TABLE time_spans(
    id UUID primary key DEFAULT uuid_generate_v4(),
    timer_id UUID not null references timers(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    started_at timestamp with time zone not null,
    stopped_at timestamp with time zone
    -- constrain start/stop time because these can be
    -- overridden by user input
    CONSTRAINT stopped_at_gt_than_or_eq_to_started_at
    CHECK (stopped_at >= started_at)
);
--;;
CREATE INDEX time_spans_timer_id_idx
ON time_spans (timer_id);
--;;
CREATE UNIQUE INDEX time_spans_timer_id_stopped_at_uniquely_null_idx
ON time_spans (timer_id, (stopped_at IS NULL))
WHERE stopped_at IS NULL;
