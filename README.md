# What is rowToDoc?

rowToDoc is a command line utility that converts the tabular _rows_ of a SQL Query, transforms them into either a JSON or XML _document_ and inserts the data directly into MarkLogic.

# Why use rowToDoc?

Migrating data from a relational database management system (RDBMS) into MarkLogic is a common data ingest requirement.  Since MarkLogic is a document database, rows from a SQL view need to be transformed into either XML or JSON.  rowToDoc is a utility built using [Spring Batch](), [MarkLogic Spring Batch]() and the [Java Client API].  

# How can I use rowToDoc?
  
## Prerequisites

  * MarkLogic 8+

## Steps

  1) [Download rowToDoc]() and unzip to your target machine
  2) Add the JDBC jar that applies to your database and add to the lib folder
  3) Change the CLASSPATH variable in the rowToDoc start script to include the jar file added in step 2
  
## Examples

  * [Import Single Table Rows into MarkLogic](./example_1.md)
  * [Import Two Joined Tables into MarkLogic](./example_2.md)

# Acknowledgments

 * [Rob Rudin](http://github.com/rjrudin)
