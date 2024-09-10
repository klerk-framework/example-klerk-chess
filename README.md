# Chess

This chess game is an example of how to build an application with Klerk.

## Getting started

Set the following environment variables:
* DATABASE_PATH=/path/to/klerk-chess.sqlite
* DEVELOPMENT_MODE=true

```
./gradlew clean build
./gradlew run
```

You can now browse to:
* http://localhost:8080/ for the game
* http://localhost:8080/admin for the auto-generated admin UI
* http://localhost:8080/graphiql for the GraphQL explorer
