#
# JBoss, Home of Professional Open Source.
# Copyright 2010, Red Hat, Inc., and individual contributors
# as indicated by the @author tags. See the copyright.txt file in the
# distribution for a full listing of individual contributors.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
#

# Environment script for setting up the JAVA_OPTS property for client instrumentation

if [[ "$#" -lt 1 || "$#" -gt 2 ]]; then
   echo "Wrong number of parameters"
   echo "Usage: setenv.sh port [ 'opentracing' ]"
   echo "(use 'opentracing' parameter if wanting to use the new prototype agent built upon the Java OpenTracing API)"
   return 1
else
    number_re='^[0-9]+$'
    if ! [[ "$1" =~ $number_re ]] ; then
       echo "Port is not a number"
       return 1
    fi
    if [[ "$#" -eq 2 && "$2" != "opentracing" ]] ; then
       echo "Second parameter should be 'opentracing'"
       return 1
    fi
fi

APM_PORT=$1
echo "APM port set to $APM_PORT"

export HAWKULAR_APM_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [[ "$#" -eq 1  ]]; then
    export HAWKULAR_APM_AGENT_JAR_PATH=$HAWKULAR_APM_HOME/hawkular-apm-agent.jar
else
    export HAWKULAR_APM_AGENT_JAR_PATH=$HAWKULAR_APM_HOME/hawkular-apm-agent-opentracing.jar
fi

# REST environment variables
export HAWKULAR_APM_URI=http://localhost:$APM_PORT
export HAWKULAR_APM_USERNAME=admin
export HAWKULAR_APM_PASSWORD=password

# General environment variables
export HAWKULAR_APM_CONFIG_REFRESH=10
# Agent logging level: FINEST, INFO, SEVERE
export HAWKULAR_APM_LOG_LEVEL=INFO

export JAVA_OPTS="-javaagent:$HAWKULAR_APM_AGENT_JAR_PATH"

# Wildfly specific
if [[ "$#" -eq 1  ]]; then
    export JAVA_OPTS="$JAVA_OPTS -Djboss.modules.system.pkgs=org.jboss.byteman,org.hawkular.apm.instrumenter,org.hawkular.apm.client.collector"
else
    export JAVA_OPTS="$JAVA_OPTS -Djboss.modules.system.pkgs=org.jboss.byteman,org.hawkular.apm.agent.opentracing,io.opentracing,org.hawkular.apm.client.opentracing"
fi