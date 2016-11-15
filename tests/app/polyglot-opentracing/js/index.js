/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const http = require('http');
const HttpDispatcher = require('httpdispatcher');
const dispatcher  = new HttpDispatcher();

const opentracing = require('opentracing');
const hawkularAPM = require('hawkular-apm-opentracing');

const SERVER_PORT = 3001;
const CONTEXT_PATH = '/nodejs';

opentracing.initGlobalTracer(new hawkularAPM.APMTracer({
    recorder: new hawkularAPM.HttpRecorder('http://hawkular-apm:9080', 'jdoe', 'password'),
    sampler: new hawkularAPM.AlwaysSampledSampler(),
}));

// /hello handler
dispatcher.onGet(`${CONTEXT_PATH}/hello`, function(req, res) {
    const serverSpan = opentracing.globalTracer().startSpan('hello', {
        childOf: extractSpanContext(opentracing.globalTracer(), req.headers),
        tags: {
            'http.method': 'GET',
            'http.url': extractUrl(req),
        }
    });

    res.writeHead(200);
    res.end('Hello from Node.js!');
    serverSpan.setTag('http.status_code', 200);
    serverSpan.finish();
});

// /createUser handler
dispatcher.onGet(`${CONTEXT_PATH}/user`, function(req, res) {
    function getRequest(parentSpan, httpOptions, name, callback) {
        const clientSpan = opentracing.globalTracer().startSpan(name, {
            childOf: parentSpan,
            tags: {
                'http.method': 'POST',
                'http.url': `http://${httpOptions.host}:${httpOptions.port}${httpOptions.path}`,
            }
        });

        http.get({
            host: httpOptions.host,
            port: httpOptions.port,
            path: httpOptions.path,
            headers: createCarrier(opentracing.globalTracer(), clientSpan),
        }, function (response) {
            clientSpan.setTag('http.status_code', response.statusCode);
            clientSpan.finish();
            callback(response);
        });
    }

    const serverSpan = opentracing.globalTracer().startSpan('get_user', {
        childOf: extractSpanContext(opentracing.globalTracer(), req.headers),
        tags: {
            'http.method': 'GET',
            'http.url': extractUrl(req),
        }
    });

    getRequest(serverSpan, {
        host: 'wildfly-swarm',
        port: 3000,
        path: '/wildfly-swarm/user'
    }, 'get_user[wildfly-swarm]', function (response) {
        serverSpan.setTag('http.status_code', 200);
        serverSpan.finish();
        res.writeHead(200);
        res.end(JSON.stringify({name: "jdoe"}));
    });
});

// create server
http.createServer(function(req, res) {
    console.log('<---' + req.url);
    dispatcher.dispatch(req, res);
}).listen(SERVER_PORT, function() {
    console.log('Server listening on: http://localhost:%s', SERVER_PORT);
});

function createCarrier(tracer, span) {
    const carrier = {};
    tracer.inject(span.context(), opentracing.FORMAT_TEXT_MAP, carrier);
    return carrier;
}

function extractSpanContext(tracer, httpHeaders) {
    return tracer.extract(opentracing.FORMAT_TEXT_MAP, httpHeaders);
}

function extractUrl(request) {
    return 'http://' + request.headers.host + request.url;
}
