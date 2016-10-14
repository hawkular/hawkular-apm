# Example application of Python with pyramid_zipkin and swagger_zipkin

Python application instrumented with pyramid_zipkin and swagger_zipkin.

## Standalone Run & Build
```shell
$ python setup.py install --user
$ python zipkin_python/app.py

($ python setup.py --help-commands)
```

## Docker
```shell
$ docker build -t hawkular/apm-example-polyglot-zipkin-python .
$ docker run -it --rm -p 3004:3004 hawkular/apm-example-polyglot-zipkin-python
```
