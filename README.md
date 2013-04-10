# PicketLink 3 Subsystem for JBoss Application Server #

This project is an extension for the JBoss Application Server that enables PicketLink for deployments.

The extension provides a set of services and deployment behaviours for most of the PicketLink projects, such as:

* [PicketLink IDM](https://github.com/picketlink/picketlink/tree/master/idm "PicketLink IDM on Github"), for Identity Management related services
* [PicketLink Core](https://github.com/picketlink/picketlink/tree/master/core "PicketLink Core on Github"), for Authentication and Authorization services for CDI applications
+ [PicketLink Federation](https://github.com/picketlink/picketlink2/federation "PicketLink Federation on Github"), for Single Sign-On, SAML and WS-Trust services for JEE applications

## System Requirements ##

The extension is currently designed and implemented considering the following requirements:

* Java 1.6
* JBoss EAP 6.1.Alpha1 (AS 7.2.0.Alpha1-redhat-4)
* Maven 3.0.3 or newer, to build and install the extension

    <b>Important note</b>
    The extension requires a specific organization for the PicketLink libraries inside the JBoss EAP modules directory. While the application server is not update, you must configure your installation in order to use the extension. See the [JBoss Application Server Configuration](#asInstallation) section for more details.

## How to build ##

You can build this project in several ways, depending of your objectives.

The simplest way to build this project and to generate the artifacts is executing the following command:

    mvn clean install
    
In this case, only the unit tests will be executed.

If you want to perform a full build, running also the integration tests you need to execute the following command:

    mvn -Dall-tests -Parquillian-managed clean install
    
## JBoss Application Server Configuration ##

### Using a Pre-configured JBoss AS Installation
The most simple and fast way to get the subsystem up and running is using the JBoss Application Server installation used during the integration tests. After executing the following command:

    mvn -Dall-tests -Parquillian-managed clean install

Navigate to the <i>target/integration-tests/containers/jboss-eap-6.1/</i> directory and use this installation to deploy your applications.

### Manual Installation

Download and install [JBoss EAP 6.1.Alpha1](http://www.jboss.org/jbossas/downloads/ "JBoss AS Downloads").

#### Installing the PicketLink Modules and Extension ####

After downloading and extracting the file, execute the following comand to install/configure the PicketLink modules and extension:

    mvn -Djboss.as.home=[JBOSS_HOME] -Pinstall-modules,install-subsystem clean install

The command above should reconfigure the PicketLink modules shipped with your JBoss AS installation with the latest configuration and libraries. Including the extension.

#### Configuring your JBoss AS installation with the PicketLink Extension and Subsystem ####

Change your standalone.xml to add an extension for the PicketLink module:

          <extensions>
                    ...
                  <extension module="org.picketlink"/>
          </extensions>
          
Now, you can configure the PicketLink Subsystem using the subsystem's domain model:

	<profile>
        <subsystem xmlns="urn:jboss:picketlink:1.0">
        	
        	<!-- Use the domain model schema to configure the subsystem -->
        	
        </subsystem>

You can get some examples about how to configure the PicketLink Subsystem from the following file:

	https://github.com/picketlink/picketlink-as-subsystem/blob/master/src/test/resources/picketlink-subsystem.xml

## How to use ##

### Identity Management Services ###

Please, follow the documentation at https://community.jboss.org/wiki/PicketLink3Subsystem#Identity_Management_Services_PicketLink_IDM.

### Authentication and Authorization Services ###

Please, follow the documentation at https://community.jboss.org/wiki/PicketLink3Subsystem#Authentication_and_Authorization_Services_PicketLink_Core.

### Federation Services ###

Please, follow the documentation at https://docs.jboss.org/author/display/PLINK/PicketLink+AS7+Subsystem.

## Running and Debugging the Integration Tests in Eclipse ##

First, import the project into your Eclipse.

Follow the instructions at [Using a Pre-configured JBoss AS Installation] (#) to get a pre-configured JBoss AS installation with all configuration required to run the integration tests.

Go to the Maven configurations for your Eclipse project and activate the following profile:

	arquillian-remote
	
Now, start your server and run one of the integration tests.