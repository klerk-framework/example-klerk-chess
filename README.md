# Chess

This chess game is an example of how to build an application with Klerk.

## Getting started

Klerk is currently residing in a private repository. Ask for a token, store it on your local file system as described here:
https://docs.gitlab.com/ee/user/packages/gradle_repository/#authenticate-to-the-package-registry-with-gradle

Make sure you have configured the project to use Java 17 or later.

Set the following environment variables:
* DATABASE_PATH=/path/to/klerk-chess.sqlite
* DEVELOPMENT_MODE=true

You can now run the main function in Application.kt. You can now browse to:
* http://localhost:8080/ for the game
* http://localhost:8080/admin for the auto-generated admin UI
* http://localhost:8080/graphiql for the GraphQL explorer
