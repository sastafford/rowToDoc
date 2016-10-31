# Create one document per table row

In this example, we are reading from a single table, transforming each row to a document, and loading directly into MarkLogic

## Rows

Open HSQL Database

    gradle customerDatabase
    
1. SQL = SELECT * FROM CUSTOMER
2. Execute SQL
3. You should see all the customers (first two rows shown below)
4. Close the Database Manager to release any locks on the database

|ID|FIRSTNAME|LASTNAME|ADDRESS|CITY|
|---|---|---|---|---|
| 0 | Laura	| Steel	| 429 Seventh Av. | Dallas |
| 1 |Susanne | King | 366 - 20th Ave. |Olten |

### Run rowToDoc

1. Go to the directory where you unzipped the rowToDoc utility.  You will run either the Windows or Unix start script depending on your operating system. 
2. Set the batch configuration parameter.  This value should not change. 
    * --config com.marklogic.spring.batch.config.RowToDoc
3. Set the HSQL parameters
    * --jdbc_driver org.hsqldb.jdbc.JDBCDriver 
        * this is the class for the HSQL JDBC driver.  If you were using a different database then you would need to point to the specific package and class name for the JDBC Driver.  
    * --jdbc_url jdbc:hsqldb:file:c:\\workspace\\rowToDoc\\data\\customers
        * This is database specific and location specific.  Change the project root directory that applies to your local environment.
    * --jdbc_username sa
    * --sql "SELECT * FROM CUSTOMER;"
4. Set the transform parameters
    * --format xml 
    * --root_local_name customer 
    * --collections customer
5. Set the MarkLogic parameters.  Change these as appropriate.  
    * --host localhost
    * --port 8155 
        * this is the port where the sample rowToDoc appserver was installed
    * --username
    * --password

EXAMPLE

         bin/rowToDoc.bat --host localhost --port 8155 --username admin --password admin --config com.marklogic.spring.batch.config.RowToDoc --jdbc_driver org.hsqldb.jdbc.JDBCDriver --jdbc_url jdbc:hsqldb:file:c:\\workspace\\rowToDoc\\data\\customers --sql "SELECT * FROM CUSTOMER;" --jdbc_username sa --format xml --root_local_name customer --collections customer
            
## Verify Documents

The following examples are the desired output.  Open up QConsole to verify.  There should be 50 customer documents.   

    <?xml version="1.0" encoding="UTF-8"?>
    <customer>
      <ID>0</ID>
      <FIRSTNAME>Laura</FIRSTNAME>
      <LASTNAME>Steel</LASTNAME>
      <STREET>429 Seventh Av.</STREET>
      <CITY>Dallas</CITY>
    </customer>
    
    <?xml version="1.0" encoding="UTF-8"?>
    <customer>
      <ID>1</ID>
      <FIRSTNAME>Susanne</FIRSTNAME>
      <LASTNAME>King</LASTNAME>
      <STREET>366 - 20th Ave.</STREET>
      <CITY>Olten</CITY>
    </customer>