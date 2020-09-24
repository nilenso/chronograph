DROP INDEX time_spans_timer_id_idx;
--;;
DROP TABLE time_spans;
--;;
DROP INDEX timers_user_id_idx;
--;;
DROP INDEX timers_task_id_idx;
--;;
DROP TABLE timers;
--;;
DROP EXTENSION "uuid-ossp"; -- fails when not run with DB Owner / super user privilege
