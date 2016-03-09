<?xml version="1.0" encoding="UTF-8"?>
<!--

    Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" version="2.0">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no" />
  <!-- <xsl:strip-space elements="*" /> -->

  <!-- //*[local-name()='config']/*[local-name()='supplement' and @name='default'] is an xPath's 1.0
       way of saying of xPath's 2.0 prefix-less selector //*:config/*:supplement[@name='default']  -->
  <xsl:template match="/*[local-name()='config']/*[local-name()='subsystem']/*[local-name()='server']/*[local-name()='connection-factory' and @name='InVmConnectionFactory']">
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">BusinessTransactions</xsl:attribute>
      <xsl:attribute name="entries">java:/BusinessTransactions</xsl:attribute>
    </xsl:element>
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">CommunicationDetails</xsl:attribute>
      <xsl:attribute name="entries">java:/CommunicationDetails</xsl:attribute>
    </xsl:element>
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">BTxnCompletionTimes</xsl:attribute>
      <xsl:attribute name="entries">java:/BTxnCompletionTimes</xsl:attribute>
    </xsl:element>
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">BTxnCompletionInformation</xsl:attribute>
      <xsl:attribute name="entries">java:/BTxnCompletionInformation</xsl:attribute>
    </xsl:element>
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">FragmentCompletionTimes</xsl:attribute>
      <xsl:attribute name="entries">java:/FragmentCompletionTimes</xsl:attribute>
    </xsl:element>
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">NodeDetails</xsl:attribute>
      <xsl:attribute name="entries">java:/NodeDetails</xsl:attribute>
    </xsl:element>
    <xsl:element name="jms-topic" namespace="{namespace-uri()}">
      <xsl:attribute name="name">Notifications</xsl:attribute>
      <xsl:attribute name="entries">java:/Notifications</xsl:attribute>
    </xsl:element>
    <xsl:copy select=".">
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <!-- copy everything else as-is -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
