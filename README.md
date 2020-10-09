# chronograph

## Setup

### Frontend Setup
You'll need:
* [`yarn`](https://classic.yarnpkg.com/en/docs/install)
* `node`
* Preferably a good ClojureScript editor of your choice

#### Running the development build with hot reloading
Run `yarn install` the first time, then `yarn start`, and finally start up the backend server (see below) and browse to http://localhost:8000/.
Although it's not necessary for hot reloading, you should connect to the REPL from your editor. Follow the instructions at https://shadow-cljs.github.io/docs/UsersGuide.html#_editor_integration.

#### Running the tests
`yarn test`. You can also run tests from the REPL using `cljs.test/run-test`.

#### Generating a release build
`yarn release`. Assets will go into the `resources/public` folder.

### Backend Setup

#### Database Provisioning

A Postgres superuser must enable the "uuid-ossp" extension as part of provisioning the DB, in staging and production. This is a one-time migration. Ref: [GCP docs on PG extensions](https://cloud.google.com/sql/docs/postgres/extensions).

```psql
CREATE EXTENSION "uuid-ossp";
```

We want Postgres to generate UUIDs for us, as default values for some fields. This is only possible with the uuid-ossp extension enabled, in Postgres 12 (or below), which we are using in GCP. Further, only a Postgres superuser may create the extension. The user that runs migrations when deploying to staging/production does NOT have superuser privileges.

For staging and production, we should create the extension at the time of provisioning since it is a one-time setup, and it is not good to expose superuser credentials for deploy-time migrations.

For local use, the DB user created by docker-compose already is a superuser, and we can configure the same in dev/test config.edns. Compare docker-compose.yml with config.dev.edn and config.test.edn, for example.

Postgres 13+ supports UUID generation out-of-the-box.

#### Local Development

- Ensure you have docker-compose installed. podman-compose will also work.
- Install **Leiningen**. The instructions for doing so, are available at https://leiningen.org/#install.
- Run docker compose to start dev & test dependencies from the root directory:

  ```bash
  docker-compose up -d
  ```

- To start a server from the REPL:
    ```clojure
    (dev.repl-utils/start-app!)
    ```
    And from the command line:
    ```bash
    lein run -- -sf config/config.dev.edn
    ```

- To run migrations from the REPL:
    ```clojure
    (chronograph.migrations/migrate)
    ```
  And from the command line:
  ```bash
  lein run -- -mf config/config.dev.edn
  ```

## Testing

To run tests, do:

```bash
lein run -- -mf config/config.test.edn
lein test
```

## Linting and formatting
We use [clj-kondo](https://github.com/borkdude/clj-kondo) for linting. To run the linter, run `clj-kondo --lint src/` and `clj-kondo --lint test/` to lint the source and test directories respectively. Please fix lint errors before pushing, or your build will fail. If you think the linter is reporting a false positive, discuss it with the team and/or add an exclusion in the configuration.

We use [cljfmt](https://github.com/weavejester/cljfmt) for formatting, with minimal extra configuration. Run `lein cljfmt fix` to fix formatting for all your source files. You should run this before you commit, or CI will complain. If you don't like how cljfmt is formatting your code, discuss it with the team and/or change the configuration.
