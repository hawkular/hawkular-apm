Hawkular-APM
============

[![Travis](https://travis-ci.org/hawkular/hawkular-apm.svg?branch=master)](https://travis-ci.org/hawkular/hawkular-apm)
[![Jira Issues](https://img.shields.io/badge/Jira-issues-blue.svg)](https://issues.jboss.org/projects/HWKAPM/issues)
[![Join the chat at freenode:hawkular](https://img.shields.io/badge/irc-freenode%3A%20%23hawkular-blue.svg)](http://webchat.freenode.net/?channels=%23hawkular)

Hawkular APM is the **Application Performance Management** solution offering:

* Distributed Tracing (e.g. for microservices)
* Application Performance Management
* Business Transaction Management (currently only using JVM javaagent based approach)

Applications can be instrumented using the [OpenTracing standard API](http://opentracing.io/), optionally with the help of
framework integrations and a non-intrusive Java Agent, that can be found
in the [OpenTracing contrib organization](https://github.com/opentracing-contrib). It is also possible to use Hawkular
APM as an alternative backend for [Zipkin client libraries](http://zipkin.io/pages/existing_instrumentations.html).

Hawkular APM provides the capabilities to monitor the flow of invocations
across servers, tiers, on-premises and in the cloud. It also enables detailed
performance analysis to be performed of the individual components that make up an
application.

Project documentation can be found [here](https://hawkular.gitbooks.io/hawkular-apm-user-guide/content/).

Build & Run
-----------

To build and run execute:

```shell
$ mvn clean install -Pdev -DskipTests [-Pitest]
$ dist/target/hawkular-apm-${version}/bin/standalone.sh [-Djboss.http.port=9411]

-> go to http://localhost:[8080|9411]/ and log in with admin:password
```

The parameters to these commands are:

* `-Djboss.http.port=9411` - bind APM port to [Zipkin](http://zipkin.io/) 9411 port. Default APM port is 8080.
* `-Pdev` - create user admin:password
* `-Pitest` - run integration tests

Add non-intrusive Java instrumentation to instrument your applications ([doc](https://hawkular.gitbooks.io/hawkular-apm-user-guide/content/installation/JVMAGENT.html)):
```shell
$ source dist/target/hawkular-apm-${version}/apm/setenv.sh <APM server port>

-> restart your Java applications
```

REST API documentation can be generated using `-Pdocgen` profile. The doc is generated under `rest/target` folder.

```shell
$ mvn clean install -Pdocgen
```

Project Structure
-----------------

The project is divided into several areas:

Folder | Description
---- | ----
**api** | This module contains the Java interfaces for services and processors, as well as the information model for exchanging business transaction information.
**client** | This folder contains the modules used to collector information from an execution environment.
**dist** | This module builds a distribution containing a pre-packaged Hawkular server with APM installed.
**examples** | This folder contains example applications.
**server** | This folder contains the modules related to the server.
**tests** | This folder contains the integration tests.
**ui** | The angularjs based user interface.

Related Repositories
--------------------
* [Hawkular-APM OpenTracing JavaScript](https://github.com/hawkular/hawkular-apm-opentracing-javascript)
* [Docker images](https://github.com/jboss-dockerfiles/hawkular-apm)
* [Documentation](https://github.com/hawkular/hawkular-apm-user-guide)

License
-------

Hawkular-APM is released under Apache License, Version 2.0 as described in the [LICENSE](LICENSE) document

```
   Copyright 2015-2017 Red Hat, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
