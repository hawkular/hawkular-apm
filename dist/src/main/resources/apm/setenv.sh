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

if [ "$#" -ne 1 ]; then
   echo "Wrong number of parameters, expected APM server port number"
   return 1
else
    number_re='^[0-9]+$'
    if ! [[ "$1" =~ $number_re ]] ; then
       echo "Port is not a number"
       return 1
    fi
fi

APM_PORT=$1
echo "APM port set to $APM_PORT"

export HAWKULAR_APM_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export HAWKULAR_APM_AGENT_JAR_PATH=$HAWKULAR_APM_HOME/hawkular-apm-agent.jar

# REST environment variables
export HAWKULAR_APM_URI=http://localhost:$APM_PORT
export HAWKULAR_APM_USERNAME=jdoe
export HAWKULAR_APM_PASSWORD=password

# General environment variables
export HAWKULAR_APM_CONFIG_REFRESH=10

export JAVA_OPTS="-javaagent:$HAWKULAR_APM_AGENT_JAR_PATH"

# Wildfly specific
export JAVA_OPTS="$JAVA_OPTS -Djboss.modules.system.pkgs=org.jboss.byteman,org.hawkular.apm.instrumenter,org.hawkular.apm.client.collector"
