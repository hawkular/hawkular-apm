# Example application of Wildfly Swarm with brave

Wildfly Swarm application instrumented with zipkin brave library.

## Standalone Run & Build
```shell
$ mvn clean install
$ java -jar target/hawkular-apm-example-zipkin-wildfly-swarm.jar -Dswarm.http.port=3003
```

## Docker
```shell
$ docker build -t hawkular/apm-example-polyglot-zipkin-wildfly-swarm .
$ docker run -it --rm -p 3003:3003 --add-host tracing-server:$TRACING_SERVER -e TRACING_PORT=$TRACING_PORT hawkular/apm-example-polyglot-zipkin-wildfly-swarm
```
