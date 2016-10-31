# What is rowToDoc?

rowToDoc is a command line utility that converts the tabular _rows_ of a SQL Query, transforms them into either a JSON or XML _document_ and inserts the data directly into MarkLogic.

# Why use rowToDoc?

Migrating data from a relational database management system (RDBMS) into MarkLogic is a common data ingest requirement.  Since MarkLogic is a document database, rows from a SQL view need to be transformed into either XML or JSON.  rowToDoc is a utility built using [Spring Batch](http://projects.spring.io/spring-batch/), [MarkLogic Spring Batch](https://github.com/sastafford/marklogic-spring-batch) and the [Java Client API](http://docs.marklogic.com/javadoc/client/index.html).  

# How do I use rowToDoc?

The best way to learn rowToDoc is by example.  

## Setup for examples

1. Download [rowToDoc]()
2. Unzip the rowToDoc zip file on your local machine

### Setup [HSQL](http://hsqldb.org/)

Clone this repository
 
    git clone https://github.com/sastafford/rowToDoc.git
 
Start the HSQL Database Manager.  Run the following command from the rowToDoc project root directory
 
    gradle customerDatabase
 
Import the Customer DDL Scripts and Insert customer data from the Database Manager
   
   1. File -> Open -> db/customer_ddl.sql
   2. File -> Open -> db/customer_insert.sql
    
### Setup [MarkLogic](http://developer.marklogic.com/products)
  
_Note: Requires MarkLogic 8+_

Setup a test database and application server.  The following script will install an application server on port 8155.  Run the following command from the project root directory

    gradle mlDeploy

If you wish to uninstall the sample database and appserver at the conclusion of the examples, execute the following command. 

    gradle mlUndeploy

## Run the Examples

  * [Create one document per table row](./example_1.md)
  * [Create one document for multiple joined rows](./example_2.md)
  
# How do I use rowToDoc with a different database?

  1) [Download rowToDoc]() and unzip to your target machine
  2) Add the database specific JDBC jar to the rowToDoc/lib folder
  3) Open the start script under rowToDoc/bin and modfiy the CLASSPATH variable to include the jar file added in step 2
  
# Acknowledgments

 * [Rob Rudin](http://github.com/rjrudin)
