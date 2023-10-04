.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING

.. _cpsStubs:


CPS Stubs
#########

.. toctree::
   :maxdepth: 1

NCMP Stubs
==========

The CPS NCMP stub module provides the capability to create dynamic and customizable stubs, offering control over the responses generated for each endpoint. This capability ensures that client interactions adhere to a specified NCMP interface, enabling comprehensive testing and validation of your applications.

The NCMP stub RestController is an extended implementation of the actual NCMP interface. It can be deployed as part of the application JAR or within a SpringBootTest JUnit environment, allowing you to define dynamic responses for each endpoint and  allowing testing against real stub interfaces.

Prerequisites
=============

Ensure you meet the following prerequisites:

1. **Required Java Installation:**
   
   Ensure that you have the required Java installed on your system. 

2. **Access to Gerrit and Maven Installation (for building CPS project locally):**

   - Ensure you have access to the ONAP Gerrit repository.
   
   - If you plan to build the CPS project locally, make sure you have Maven installed. 

Method 1: Running Stubs as an Application
=========================================

Follow these steps to run the CPS-NCMP stub application:

1. **Download Application Jar:**

   You can obtain the CPS-NCMP stub application jar in one of the following ways:

   - **Option 1: Download from Nexus Repository:**

     Download the application jar from the Nexus repository at `https://nexus.onap.org/content/repositories/releases/org/onap/cps/cps-ncmp-rest-stub-app/`_.

   - **Option 2: Build Locally:**

     To build the CPS project locally, navigate to the project's root directory. Once there, you can build the project, and the application CPS-NCMP stub application jar can be found in the following location:

     ::

       cps/cps-ncmp-rest-stub/cps-ncmp-rest-stub-app/target/

2. **Run the Application:**

   After obtaining the application jar, use the following command to run it:

   .. code-block:: bash

      java -jar ./cps-ncmp-rest-stub-app-<VERSION>.jar

   Replace ``<VERSION>`` with the actual version number of the application jar.

This will start the CPS-NCMP stub application, and you can interact with it as needed.

.. _`https://nexus.onap.org/content/repositories/releases/org/onap/cps/cps-ncmp-rest-stub-app/`: https://nexus.onap.org/content/repositories/releases/org/onap/cps/cps-ncmp-rest-stub-app/

Method 2: Using Stubs in Unit Tests
===================================
1. **Add Dependency to pom.xml:**

   To include the required module in your project, add the following dependency to your `pom.xml` file:

   .. code-block:: xml

      <dependency>
        <groupId>org.onap.cps</groupId>
        <artifactId>cps-ncmp-rest-stub-service</artifactId>
        <version>VERSION</version>
     </dependency>

   Replace ``VERSION`` with the actual version number.

2. **Using Custom Response Objects:**

   If you prefer to use custom response objects instead of the built-in ones, follow these steps:

   Modify the `application.yaml` file located in your project's test resources directory (`src/test/resources`).

   Add the following property to the `application.yaml` file:
   
   .. code-block:: yaml

      stub:
          path: "/stubs/"

   This property configuration enables the use of custom response objects.

   **Note:** Custom response objects can be placed in the `src/test/resources` directory of your project. Refer to the `this <https://github.com/onap/cps/tree/master/cps-ncmp-rest-stub/cps-ncmp-rest-stub-service/src/main/resources/stubs>`_ for examples.

**Custom Responses for Supported Endpoints**

  Only the following endpoints are supported for the first draft. To use your custom response objects for these endpoints, create the corresponding JSON files:

  - For RequestMethod.GET /v1/ch/{cm-handle}/data/ds/{datastore-name}, create "passthrough-operational-example.json".

  - For RequestMethod.POST /v1/ch/searches, create "cmHandlesSearch.json".
