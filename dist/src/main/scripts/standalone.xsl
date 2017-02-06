<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
    and other contributors as indicated by the @author tags.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:ds="urn:jboss:domain:datasources:3.0" xmlns:ra="urn:jboss:domain:resource-adapters:3.0" xmlns:ejb3="urn:jboss:domain:ejb3:3.0"
  xmlns:logging="urn:jboss:domain:logging:3.0" xmlns:undertow="urn:jboss:domain:undertow:3.0" xmlns:tx="urn:jboss:domain:transactions:3.0"
  xmlns:messaging="urn:jboss:domain:messaging-activemq:1.0"
  version="2.0" exclude-result-prefixes="xalan ds ra ejb3 logging undertow tx messaging">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
  <xsl:strip-space elements="*"/>

  <!-- copy everything else as-is -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//messaging:subsystem/*[local-name()='server' and @name='default']">
    <xsl:copy>
      <xsl:attribute name="name">default</xsl:attribute>
      <statistics enabled="true"/>
      <xsl:apply-templates select="@*|node()"/>

      <pooled-connection-factory name="apmpcf" connectors="in-vm" entries="java:/APMJMSCF" max-pool-size="1000"/>
      <jms-topic name="Traces" entries="java:/Traces"/>
      <jms-topic name="CommunicationDetails" entries="java:/CommunicationDetails"/>
      <jms-topic name="TraceCompletions" entries="java:/TraceCompletions"/>
      <jms-topic name="TraceCompletionInformation" entries="java:/TraceCompletionInformation"/>
      <jms-topic name="NodeDetails" entries="java:/NodeDetails"/>
      <jms-topic name="Notifications" entries="java:/Notifications"/>
      <jms-topic name="Spans" entries="java:/Spans"/>
      <jms-topic name="SpanTraceCompletionProcessing" entries="java:/SpanTraceCompletionProcessing"/>
    </xsl:copy>
  </xsl:template>

  <!-- Enable property replacement on MDB annotations -->
  <xsl:template match="node()[name(.)='spec-descriptor-property-replacement'][last()]">

    <xsl:variable name="enablePropertyReplacement">
      <annotation-property-replacement>true</annotation-property-replacement>
    </xsl:variable>

    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
    <xsl:copy-of select="$enablePropertyReplacement" />

  </xsl:template>

  <!-- Add new cache container -->
  <xsl:template match="node()[name(.)='cache-container'][last()]">

    <xsl:variable name="newCacheContainer">
      <cache-container name="apm" jndi-name="infinispan/APM">
        <local-cache name="communicationdetails"/>
        <local-cache name="communicationdetailsMulticonsumer"/>
        <local-cache name="producerinfo"/>
        <local-cache name="span" />
        <local-cache name="spanTrace" />
        <local-cache name="spanChildren"/>
      </cache-container>
    </xsl:variable>

    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
    <xsl:copy-of select="$newCacheContainer" />

  </xsl:template>

  <xsl:template match="//*[*[local-name()='root-logger']]">
    <xsl:copy>
      <xsl:copy-of select="node()|@*"/>
      <logger category="org.hawkular.apm">
        <level name="${{hawkular.log.apm:INFO}}" />
      </logger>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
