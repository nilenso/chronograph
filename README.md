# chronograph

## Setup

### Frontend Setup
You'll need:
* [`yarn`](https://classic.yarnpkg.com/en/docs/install)
* `node`
* Preferably a good ClojureScript editor of your choice

#### Running the development build with hot reloading
First, set `chronograph-web.config/google-client-id` to your Google OAuth client ID.
Then `yarn start`, and finally start up the backend server (see below) and browse to http://localhost:8000/.
Although it's not necessary for hot reloading, you should connect to the REPL from your editor. Follow the instructions at https://shadow-cljs.github.io/docs/UsersGuide.html#_editor_integration.

#### Running the tests
`yarn test`. You can also run tests from the REPL using `cljs.test/run-test`.

#### Generating a release build
`yarn release`. Assets will go into the `resources/public` folder.

### Backend Setup
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

## Testing

To run tests, do:

```bash
lein run -- -mf config/config.test.edn
lein test
```
