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

CREATE KEYSPACE IF NOT EXISTS hawkular_btm WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- #

CREATE TABLE IF NOT EXISTS hawkular_btm.businesstransactions (
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

CREATE INDEX IF NOT EXISTS ON hawkular_btm.businesstransactions (id);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.businesstransactions (businessTransaction);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.businesstransactions (properties);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.businesstransactions (correlationIds);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.businesstransactions (hostName);

-- #

CREATE TABLE IF NOT EXISTS hawkular_btm.nodedetails (
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

CREATE INDEX IF NOT EXISTS ON hawkular_btm.nodedetails (id);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.nodedetails (businessTransaction);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.nodedetails (properties);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.nodedetails (hostName);

-- #

CREATE TABLE IF NOT EXISTS hawkular_btm.completiontimes (
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

CREATE INDEX IF NOT EXISTS ON hawkular_btm.completiontimes (id);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.completiontimes (businessTransaction);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.completiontimes (properties);

-- #

CREATE INDEX IF NOT EXISTS ON hawkular_btm.completiontimes (fault);

-- #

CREATE TABLE IF NOT EXISTS hawkular_btm.businesstxnconfig (
    tenantId text,
    id text,
    doc text,
    PRIMARY KEY (tenantId, id)
);

-- #

CREATE TABLE IF NOT EXISTS hawkular_btm.businesstxnconfiginvalid (
    tenantId text,
    id text,
    doc text,
    PRIMARY KEY (tenantId, id)
);

