# Chess

This chess game is an example of how to build an application with Klerk. Typically, Klerk would not be used to create
games, but a chess game is a well known example of a system that has non-trivial rules and states and therefore makes
a good example. 

## Getting started

Set the Project SDK to Java 17 (Open Module Settings -> Project).

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

## About the game

The game illustrates how to declare chess rules in Klerk and how it is then possible to query Klerk for the rules.

### There is barely any chess knowledge in the UI

The user interface is written to be as slim as possible. This means that no chess rules are implemented in the UI. 
Instead, it queries Klerk to figure out what to show on the screen. More specifically:
* The UI can draw a chess board
* It allows the user to drag a piece. When the piece is dropped, the UI asks Klerk "what would happen if the user would 
make a move from square <start> to square <end>?". It then renders the result.
* The UI can resend the request saying "this time I mean it".
* It knows that pieces should only be draggable when MakeMove is possible
* It knows that it should reload the page whenever anything happens in the game (using server-sent events).

There is a lot that the user interface _doesn't_ know:
* How can a bishop move?
* What should happen when the user castles?
* How to promote a pawn.


### The admin UI can render a documentation

A state diagram for the chess game can be found under the _documentation_ section in the auto-generated admin UI. You
can also find information about the events, such as which values are valid in the event parameters.


### A GraphQL API can be generated

There GraphQL API obeys all the rules that you have configured.


### The AI is stupid

The computer plays as _Mr. Robot_. The application subscribes to game events and when it is Mr. Robot's turn, a
background job is scheduled. The job waits 4 seconds and then makes a random (valid) move.

### Authentication is assumed
This example does not show how to authenticate the user. Authentication is something that typically happens outside 
Klerk.
