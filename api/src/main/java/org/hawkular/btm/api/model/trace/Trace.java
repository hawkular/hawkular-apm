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
package org.hawkular.btm.api.model.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hawkular.btm.api.model.trace.CorrelationIdentifier.Scope;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents the top level trace instance
 * description. This model can be used to represent either a fragment
 * of a trace instance, captured from one of multiple
 * resources executing the transaction, or a complete end to end correlated
 * description of the flow.
 *
 * @author gbrown
 *
 */
public class Trace {

    @JsonInclude
    private String id;

    @JsonInclude
    private long startTime;

    @JsonInclude(Include.NON_EMPTY)
    private String businessTransaction;

    @JsonInclude(Include.NON_EMPTY)
    private String principal;

    @JsonInclude(Include.NON_EMPTY)
    private String hostName;

    @JsonInclude(Include.NON_EMPTY)
    private String hostAddress;

    @JsonInclude
    private List<Node> nodes = new ArrayList<Node>();

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> properties = new HashMap<String, String>();

    public Trace() {
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @return The trace
     */
    public Trace setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     * @return The trace
     */
    public Trace setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * @return the business transaction name
     */
    public String getBusinessTransaction() {
        return businessTransaction;
    }

    /**
     * @param name the business transaction name to set
     * @return The trace
     */
    public Trace setBusinessTransaction(String name) {
        this.businessTransaction = name;
        return this;
    }

    /**
     * @return the principal
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * @param principal the principal to set
     * @return The trace
     */
    public Trace setPrincipal(String principal) {
        this.principal = principal;
        return this;
    }

    /**
     * @return the host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @param hostName the host name to set
     * @return The trace
     */
    public Trace setHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    /**
     * @return the host address
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * @param hostAddress the host address to set
     * @return The trace
     */
    public Trace setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
        return this;
    }

    /**
     * @return the nodes
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     * @return The trace
     */
    public Trace setNodes(List<Node> nodes) {
        this.nodes = nodes;
        return this;
    }

    /**
     * This method returns properties that can be used to search for
     * the trace.
     *
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     * @return The trace
     */
    public Trace setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    /**
     * This method determines whether this trace is the initial
     * fragment of an instance.
     *
     * @return Whether this is the initial fragment
     */
    public boolean initialFragment() {
        // Initial fragment, if the first node has no 'interaction' based correlation ids
        return !getNodes().isEmpty() && getNodes().get(0).getCorrelationIds(Scope.Interaction).isEmpty();
    }

    /**
     * This method returns the end time of the trace fragment.
     *
     * @return The end time (in milliseconds)
     */
    public long endTime() {
        return getStartTime() + calculateDuration();
    }

    /**
     * This method returns the duration of the trace fragment.
     *
     * @return The duration (in milliseconds), or 0 if no nodes defined
     */
    public long calculateDuration() {
        if (!nodes.isEmpty()) {
            long endTimeNS = 0;

            for (int i = 0; i < getNodes().size(); i++) {
                Node node = getNodes().get(i);
                long et = node.overallEndTime();

                if (et > endTimeNS) {
                    endTimeNS = et;
                }
            }

            long elapsedTime = endTimeNS - getNodes().get(0).getBaseTime();

            // Convert elapsed time to milliseconds
            return TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
        }

        return 0L;
    }

    /**
     * This method locates any node within the trace that
     * is associated with the supplied correlation id.
     *
     * @param cid The correlation identifier
     * @return The nodes that were correlated with the supplied correlation identifier
     */
    public Set<Node> getCorrelatedNodes(CorrelationIdentifier cid) {
        Set<Node> ret = new HashSet<Node>();

        for (Node n : getNodes()) {
            n.findCorrelatedNodes(cid, ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((businessTransaction == null) ? 0 : businessTransaction.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        result = prime * result + ((principal == null) ? 0 : principal.hashCode());
        result = prime * result + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Trace other = (Trace) obj;
        if (hostAddress == null) {
            if (other.hostAddress != null) {
                return false;
            }
        } else if (!hostAddress.equals(other.hostAddress)) {
            return false;
        }
        if (hostName == null) {
            if (other.hostName != null) {
                return false;
            }
        } else if (!hostName.equals(other.hostName)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (businessTransaction == null) {
            if (other.businessTransaction != null) {
                return false;
            }
        } else if (!businessTransaction.equals(other.businessTransaction)) {
            return false;
        }
        if (nodes == null) {
            if (other.nodes != null) {
                return false;
            }
        } else if (!nodes.equals(other.nodes)) {
            return false;
        }
        if (principal == null) {
            if (other.principal != null) {
                return false;
            }
        } else if (!principal.equals(other.principal)) {
            return false;
        }
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }
        if (startTime != other.startTime) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Trace [id=" + id + ", startTime=" + startTime
                + ", businessTransaction=" + businessTransaction + ", principal="
                + principal + ", hostName=" + hostName + ", hostAddress=" + hostAddress + ", nodes=" + nodes
                + ", properties=" + properties + "]";
    }

}
