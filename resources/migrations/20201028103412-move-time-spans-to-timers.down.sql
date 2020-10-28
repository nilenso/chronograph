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
--;;
ALTER TABLE timers DROP COLUMN time_spans;
