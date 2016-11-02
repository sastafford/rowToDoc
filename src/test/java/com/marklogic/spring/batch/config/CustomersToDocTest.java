package com.marklogic.spring.batch.config;

import com.marklogic.client.admin.TransformExtensionsManager;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.FileHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.junit.Fragment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Handling data as a column map is perfect for a POC-style project where it's far more important to quickly get data
 * from a SQL database into MarkLogic than it is to generate precise XML. Using a column map avoids the need to create
 * Java objects and map them to tables.
 * 
 * This test class verifies the following:
 * <ol>
 * <li>Read rows with a JOIN as a column map (Map<String,Object>)</li>
 * <li>Process a forward slash in a column label to produce a nested XML element</li>
 * <li>Merge rows that have the same ID</li>
 * <li>Write merged rows as a single document to MarkLogic, with or without nested elements</li>
 * </ol>
 */
public class CustomersToDocTest extends AbstractRowToDocTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private TransformExtensionsManager transMgr;
    private String sql = "SELECT customer.*, invoice.id as \"invoice/id\", invoice.total as \"invoice/total\" FROM invoice LEFT JOIN customer on invoice.customerId = customer.id ORDER BY customer.id";

    @Before
    public void setup() throws IOException {
        createInMemoryDatabase("db/sampledata_ddl.sql", "db/sampledata_insert.sql");

        clientTestHelper.setDatabaseClientProvider(getClientProvider());

        Resource transform = getApplicationContext().getResource("classpath:/transforms/simple.xqy");
        transMgr = getClient().newServerConfigManager().newTransformExtensionsManager();
        FileHandle fileHandle = new FileHandle(transform.getFile());
        fileHandle.setFormat(Format.XML);
        transMgr.writeXQueryTransform("simple", fileHandle);

    }

    @After
    public void teardown() {
        shutdownInMemoryDatabase();
        transMgr.deleteTransform("simple");
    }

    @Test
    public void transferSingleCustomerRowToMarkLogic() {
        String sql = "SELECT customer.* FROM customer WHERE customer.id = 0";
        runRowToDoc(sql, "xml", "customer", "customer");
        clientTestHelper.assertCollectionSize("Expecting 1 customer docs", "customer", 1);
    }

    @Test
    public void transferCustomerTableToMarkLogic() {
        String sql = "SELECT customer.* FROM customer";
        runRowToDoc(sql, "xml", "customer", "customer");
        clientTestHelper.assertCollectionSize("Expecting 50 customer docs", "customer", 50);
    }

    @Test
    public void transferCustomersWithInvoicesTest() {
        String sql = "SELECT customer.*, invoice.id as \"invoice/id\", invoice.total as \"invoice/total\" FROM invoice LEFT JOIN customer on invoice.customerId = customer.id ORDER BY customer.id";
        runRowToDoc(sql, "xml", "invoice", "invoice");
        Fragment f = loadInvoice();
        f.assertElementValue("/invoice/ID", "13");
        f.assertElementValue("/invoice/LASTNAME", "Ringer");
        f.assertElementExists("/invoice/invoice[1]/total[. = '3215']");
        f.assertElementExists("/invoice/invoice[2]/total[. = '1376']");
    }

    @Test
    public void transferCustomersWithInvoicesAndTransformTest() {
        String sql = "SELECT customer.*, invoice.id as \"invoice/id\", invoice.total as \"invoice/total\" FROM invoice LEFT JOIN customer on invoice.customerId = customer.id ORDER BY customer.id";
        runRowToDocWithTransform(sql, "xml", "invoice", "invoice", "simple", "monster,grover,trash,oscar");
        Fragment f = loadInvoice();
        f.assertElementValue("/invoice/invoice/ID", "13");
        f.assertElementValue("/invoice/invoice/LASTNAME", "Ringer");
        f.assertElementExists("/invoice/invoice/invoice[1]/total[. = '3215']");
        f.assertElementExists("/invoice/invoice/invoice[2]/total[. = '1376']");
        f.assertElementExists("/invoice/transform");
        f.assertElementExists("/invoice/monster[. = 'grover']");
        f.assertElementExists("/invoice/trash[. = 'oscar']");
    }

    @Test
    public void transferCustomersWithInvoicesAndTransformWithoutTransformParamsTest() {
        String sql = "SELECT customer.*, invoice.id as \"invoice/id\", invoice.total as \"invoice/total\" FROM invoice LEFT JOIN customer on invoice.customerId = customer.id ORDER BY customer.id";
        runRowToDocWithTransformAndNoTransformParams(sql, "xml", "invoice", "invoice", "simple");
        Fragment f = loadInvoice();
        f.assertElementValue("/invoice/invoice/ID", "13");
        f.assertElementValue("/invoice/invoice/LASTNAME", "Ringer");
        f.assertElementExists("/invoice/invoice/invoice[1]/total[. = '3215']");
        f.assertElementExists("/invoice/invoice/invoice[2]/total[. = '1376']");
        f.assertElementExists("/invoice/transform");
        f.assertElementMissing("Monster element expected to be missing", "/invoice/monster[. = 'grover']");
        f.assertElementMissing("trash element expected to be missing", "/invoice/trash[. = 'oscar']");
    }

    private Fragment loadInvoice() {
        XMLDocumentManager mgr = getClient().newXMLDocumentManager();
        String xml = mgr.read("/invoice/13.xml", new StringHandle()).get();
        return parse(xml);
    }

    @Configuration
    public static class RowToDocTest extends RowToDoc {
        @Override
        protected DataSource buildDataSource() {
            return embeddedDatabase;
        }
    }

}
