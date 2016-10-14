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

FROM cassandra:3.7

MAINTAINER Pavol Loffay <ploffay@redhat.com>

ENV APP_HOME /app/
ENV CASSANDRA_HOME /usr/share/cassandra/lib/
ENV CASSANDRA_CLUSTER_NAME cassandra-wildfly-swarm

# install dependencies of install-cassandra-zipkin.sh
RUN apt-get update && apt-get install -y wget git
# install maven
ENV MAVEN_DISTRIBUTION apache-maven-3.3.9
ADD http://tux.rainside.sk/apache/maven/maven-3/3.3.9/binaries/$MAVEN_DISTRIBUTION-bin.tar.gz /tmp/
RUN mkdir /opt/maven && tar xvf /tmp/$MAVEN_DISTRIBUTION-bin.tar.gz -C /opt/maven
ENV PATH /opt/maven/$MAVEN_DISTRIBUTION/bin/:$PATH
# install JDK
# set shell variables for java installation
ENV JAVA_VERSION 1.8.0_11
ENV JAVA_FILENAME jdk-8u11-linux-x64.tar.gz
ENV JAVA_DOWNLOAD_LINK http://download.oracle.com/otn-pub/java/jdk/8u11-b12/$JAVA_FILENAME
# download java, accepting the license agreement
RUN wget --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" -O /tmp/$JAVA_FILENAME $JAVA_DOWNLOAD_LINK
# unpack java
RUN mkdir /opt/java-oracle && tar -zxf /tmp/$JAVA_FILENAME -C /opt/java-oracle/
ENV JAVA_HOME /opt/java-oracle/jdk$JAVA_VERSION
ENV PATH $PATH:$JAVA_HOME/bin

# install tracing jars to C*
WORKDIR $APP_HOME
ADD install-cassandra-zipkin.sh $APP_HOME
RUN bash $APP_HOME/install-cassandra-zipkin.sh
RUN cp $APP_HOME/target/lib/*.jar $CASSANDRA_HOME

EXPOSE 9042 9160 7000 7001
