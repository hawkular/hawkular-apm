# Example of cassandra-zipkin-tracing

Cassandra with zipkin instrumentation.

## Docker
```shell
$ docker build -t hawkular/apm-example-polyglot-zipkin-cassandra .
$ dcker run -it --rm -p 9042:9042 --add-host tracing-server:$TRACING_SERVER -e JVM_OPTS="-Dcassandra.custom_tracing_class=com.thelastpickle.cassandra.tracing.ZipkinTracing -Dcassandra.custom_query_handler_class=org.apache.cassandra.cql3.CustomPayloadMirroringQueryHandler -DZipkinTracing.httpCollectorHost=$TRACING_SERVER -DZipkinTracing.httpCollectorPort=$TRACING_PORT" hawkular/apm-example-polyglot-zipkin-cassandra
```
