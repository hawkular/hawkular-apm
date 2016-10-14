# Example application using zipkin-tracer

Ruby example application instrumented with zipkin-tracer library.

Application is using [Roda](http://roda.jeremyevans.net/index.html) framework to build
microservices in Ruby. 

Tracing can be enabled for any Ruby application using [Rack](http://rack.github.io/).

## Standalone Run & Build
```shell
$ bundle install
$ bundle exec rackup -p 3002 --host 0.0.0.0 -s puma
```

## Docker
```shell
$ docker build -t hawkular/apm-example-polyglot-zipkin-ruby .
$ docker run -it --rm -p 3002:3002 --add-host tracing-server:$TRACING_SERVER -e TRACING_PORT=$TRACING_PORT hawkular/apm-example-polyglot-zipkin-ruby
```
