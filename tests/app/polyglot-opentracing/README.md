# Polyglot OpenTracing Example

## Build & Run
```shell
$ mvn clean install
$ docker-compose up --build
```

## Example Requests
```shell
$ curl -ivX GET 'http://localhost:3001/nodejs/user'
```

## Clean it up
```shell
$ docker-compose down --rmi local
```
