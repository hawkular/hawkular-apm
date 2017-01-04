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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:ds="urn:jboss:domain:datasources:2.0"
                xmlns:ra="urn:jboss:domain:resource-adapters:2.0"
                xmlns:ejb3="urn:jboss:domain:ejb3:2.0"
                version="2.0"
                exclude-result-prefixes="xalan ds ra ejb3">

  <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" xalan:indent-amount="4" standalone="no"/>
  <xsl:strip-space elements="*"/>

  <!-- copy everything else as-is -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*" />
    </xsl:copy>
  </xsl:template>

  <!-- Add new secure-deployment -->
  <xsl:template match="node()[name(.)='secure-deployment'][last()]">

    <xsl:variable name="newSecureDeployment">
      <secure-deployment name="hawkular-apm-server.war">
        <realm>hawkular</realm>
        <resource>hawkular-accounts-backend</resource>
        <use-resource-role-mappings>true</use-resource-role-mappings>
        <enable-cors>true</enable-cors>
        <enable-basic-auth>true</enable-basic-auth>
        <credential name="secret"><xsl:value-of select="node()[name(.)='credential'][last()]" /></credential>
      </secure-deployment>
    </xsl:variable>

    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
    <xsl:copy-of select="$newSecureDeployment" />

  </xsl:template>

</xsl:stylesheet>