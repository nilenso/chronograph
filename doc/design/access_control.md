# Access Control

This document describes how user access control is implemented.

Given the limited flexibility we need, we are using a set of hardcoded roles.
However, these can be parameterized. For now, the parameter itself is
hard-coded as well for optimal DB access.

These are effectively ACLS.

### Roles

| Role   | Parameter 1     | Access                                             |
| ---    | --              | --                                                 |
| Admin  | organization_id | Full access to all resources under an organization |
| Member | organization_id | Full access to own timers under an organization    |


### DB Schema

__Tablename__ acl

__Columns__

| Column          | Type |
| -------         | ---- |
| user_id         | fkey |
| organization_id | fkey |
| role            | enum |

__Indexes__

Unique index on (user_id, organization_id)

