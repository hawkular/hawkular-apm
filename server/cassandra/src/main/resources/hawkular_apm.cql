--
-- Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
-- and other contributors as indicated by the @author tags.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- #

CREATE KEYSPACE IF NOT EXISTS hawkular_apm WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- #

CREATE TABLE IF NOT EXISTS hawkular_apm.traces (
    tenantId text,
    datetime timestamp,
    id text,
    businessTransaction text,
    hostName text,
    properties list<text>,
    correlationIds list<text>,
    doc text,
    PRIMARY KEY (tenantId, datetime, id)
);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.traces (id);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.traces (businessTransaction);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.traces (properties);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.traces (correlationIds);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.traces (hostName);

-- #

CREATE TABLE IF NOT EXISTS hawkular_apm.nodedetails (
    tenantId text,
    datetime timestamp,
    key uuid,
    id text,
    businessTransaction text,
    uri text,
    componentType text,
    operation text,
    fault text,
    hostName text,
    properties list<text>,
    details list<text>,
    correlationIds list<text>,
    doc text,
    PRIMARY KEY (tenantId, datetime, key)
);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.nodedetails (id);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.nodedetails (businessTransaction);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.nodedetails (properties);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.nodedetails (hostName);

-- #

CREATE TABLE IF NOT EXISTS hawkular_apm.completiontimes (
    tenantId text,
    datetime timestamp,
    key uuid,
    id text,
    businessTransaction text,
    fault text,
    properties list<text>,
    doc text,
    PRIMARY KEY (tenantId, datetime, key)
);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.completiontimes (id);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.completiontimes (businessTransaction);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.completiontimes (properties);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_apm.completiontimes (fault);

-- #

CREATE TABLE IF NOT EXISTS hawkular_apm.businesstxnconfig (
    tenantId text,
    id text,
    doc text,
    PRIMARY KEY (tenantId, id)
);

-- #

CREATE TABLE IF NOT EXISTS hawkular_apm.businesstxnconfiginvalid (
    tenantId text,
    id text,
    doc text,
    PRIMARY KEY (tenantId, id)
);

