<?xml version="1.0" encoding="UTF-8"?>
<!-- XSLT file to add the security domains to the standalone.xml used during 
	the integration tests. -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:as="urn:jboss:domain:2.0" xmlns:sd="urn:jboss:domain:datasources:2.0"
	version="1.0">

	<xsl:output method="xml" indent="yes" />

	<xsl:template
		match="//as:profile/sd:subsystem/sd:datasources/sd:datasource[@pool-name='ExampleDS']" />
	<xsl:template
		match="//as:profile/sd:subsystem/sd:datasources/sd:datasource[@pool-name='StagingExampleDS']" />

	<xsl:template match="as:profile/sd:subsystem/sd:datasources">
		<datasources>
			<datasource jndi-name="java:jboss/datasources/ExampleDS"
				pool-name="ExampleDS" enabled="true" use-java-context="true">
				<connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1</connection-url>
				<driver>h2</driver>
				<security>
					<user-name>sa</user-name>
					<password>sa</password>
				</security>
			</datasource>
      <datasource jndi-name="java:jboss/datasources/ExampleDS2"
                  pool-name="ExampleDS2" enabled="true" use-java-context="true">
        <connection-url>jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1</connection-url>
        <driver>h2</driver>
        <security>
          <user-name>sa</user-name>
          <password>sa</password>
        </security>
      </datasource>
      <datasource jndi-name="java:jboss/datasources/ExampleDS3"
                  pool-name="ExampleDS3" enabled="true" use-java-context="true">
        <connection-url>jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1</connection-url>
        <driver>h2</driver>
        <security>
          <user-name>sa</user-name>
          <password>sa</password>
        </security>
      </datasource>
      <datasource jndi-name="java:jboss/datasources/ExampleDS4"
                  pool-name="ExampleDS4" enabled="true" use-java-context="true">
        <connection-url>jdbc:h2:mem:test4;DB_CLOSE_DELAY=-1</connection-url>
        <driver>h2</driver>
        <security>
          <user-name>sa</user-name>
          <password>sa</password>
        </security>
      </datasource>
      <datasource jndi-name="java:jboss/datasources/ExampleDS5"
                  pool-name="ExampleDS5" enabled="true" use-java-context="true">
        <connection-url>jdbc:h2:mem:test5;DB_CLOSE_DELAY=-1</connection-url>
        <driver>h2</driver>
        <security>
          <user-name>sa</user-name>
          <password>sa</password>
        </security>
      </datasource>
			<xsl:apply-templates select="@* | *" />
		</datasources>
	</xsl:template>

	<!-- Copy everything else. -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>