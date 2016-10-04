#!/usr/bin/python
# -*- coding: utf-8 -*-
#
# Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
# and other contributors as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys
import logging

from pyramid.config import Configurator
from pyramid.response import Response
from pyramid.view import view_config
from waitress import serve

from kafka import SimpleProducer, SimpleClient

from bravado.client import SwaggerClient
from swagger_zipkin.zipkin_decorator import ZipkinClientDecorator

log = logging.getLogger(__file__)

PORT = 3004
BASE_PATH = 'pyramid'

@view_config(route_name='hello')
def hello_handler(request):
    logging.info('hello request')
    return Response('Hello from Pyramid! [Python]')


@view_config(route_name='user', request_method='GET')
def user_handler(request):
    print('User created')
    return Response('User created!')


@view_config(route_name='loop')
def loop_handler(request):
    spec_dict = {
        "swagger": "2.0",
        "info": {
            "version": "0.1",
            "title": "Hawkular APM zipkin pyramid API"
        },
        "host": "127.0.0.1:" + str(PORT),
        "schemes": [
            "http"
        ],
        "basePath": "/" + BASE_PATH,
        "tags": [
            {
                "name": "hello"
            }
        ],
        "paths": {
            "/hello": {
                "get": {
                    "tags": [
                        "hello"
                    ],
                    "operationId": "get",
                    "responses": {
                        "200": {
                            "description": "OK"
                        },
                    }
                }
            }
        }
    }

    client = SwaggerClient.from_spec(spec_dict=spec_dict)
    zipkin_wrapped_client = ZipkinClientDecorator(client)
    zipkin_wrapped_client.hello.get().result()

    return Response('loop -> hello')


def kafka_handler(stream_name, message):
    kafka = SimpleClient('{0}:{1}'.format('kafka', 9092))
    producer = SimpleProducer(kafka, async=True)
    producer.send_messages(stream_name, message)


def main(global_config, **settings):
    settings['service_name'] = 'pyramid'
    settings['zipkin.transport_handler'] = kafka_handler
    settings['zipkin.stream_name'] = 'zipkin'
    settings['zipkin.tracing_percent'] = 100.0

    config = Configurator(settings=settings, route_prefix=BASE_PATH)
    config.include('pyramid_zipkin')

    config.add_route('hello', '/hello')
    config.add_route('loop', '/loop')
    config.add_route('user', '/users')
    config.scan()

    app = config.make_wsgi_app()
    serve(app, host='0.0.0.0', port=PORT)

if __name__ == "__main__":
    main(sys.argv)

