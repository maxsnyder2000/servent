# **Servent**: A Unified **SERV**er-Cli**ENT** Semantics

Requirements:
- [GNU Make](https://www.gnu.org/software/make/manual/make.html)
- [Scala Build Tool](https://www.scala-sbt.org/)
- [Apache Maven](https://maven.apache.org/)
- [Node Package Manager](https://www.npmjs.com/)
- [PostgreSQL](https://www.postgresql.org/)

To compile `MAIN.js`, use `make compile`.

To deploy the server, use `make server`. Edits to files in `/servent-service` will trigger a re-deploy.

To deploy the client, use `make client`. Edits to files in `/servent-ui` will trigger a re-deploy.

To execute all simultaneously, use `make -j`. Alternatively, execute each in a separate shell.
