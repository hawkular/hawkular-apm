# Java JAX-RS application instrumented with OpenTracing API

## Build & Run
```shell
$ mvn clean install

$ export HAWKULAR_APM_URI=http://localhost:8080
$ export HAWKULAR_APM_USERNAME=jdoe
$ export HAWKULAR_APM_PASSWORD=password
$ java -jar target/hawkular-apm-tests-app-polyglot-opentracing-swarm-swarm.jar -Dswarm.http.port=3000
```

## Example Requests
```shell
$ curl -ivX GET 'http://localhost:3000/hello'
$ curl -ivX GET 'http://localhost:3000/user'
```
