package com.marklogic.spring.batch.config;

import com.marklogic.junit.ClientTestHelper;
import com.marklogic.spring.batch.test.AbstractJobTest;
import com.marklogic.spring.batch.test.JobProjectTestConfig;
import org.junit.After;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;

/**
 * Provides some support for tests that need to stand up an embedded HSQL database.
 */
@ContextConfiguration(classes = {JobProjectTestConfig.class})
public abstract class AbstractRowToDocTest extends AbstractJobTest {

    protected static EmbeddedDatabase embeddedDatabase;

    protected ClientTestHelper clientTestHelper = new ClientTestHelper();

    public void teardown() {
        if (embeddedDatabase != null) {
            embeddedDatabase.shutdown();
        }
        embeddedDatabase = null;
    }

    protected void createInMemoryDatabase(String... scripts) {
        embeddedDatabase = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL).addScripts(scripts).build();
    }

    protected void runRowToDoc(String sql, String format, String rootName, String collections) {
        runJob(RowToDocTest.class,
                "--sql", sql,
                "--jdbc_username", "sa",
                "--format", format,
                "--root_local_name", rootName,
                "--collections", collections
        );

    }

    protected void runRowToDocWithTransform(String sql, String format,
                                            String rootName, String collections,
                                            String transformName,
                                            String transformParameters) {
        runJob(RowToDocTest.class,
                "--sql", sql,
                "--jdbc_username", "sa",
                "--format", format,
                "--root_local_name", rootName,
                "--collections", collections,
                "--transform_name", transformName,
                "--transform_parameters", transformParameters
        );

    }

    @Configuration
    public static class RowToDocTest extends RowToDoc {
        @Override
        protected DataSource buildDataSource() {
            return embeddedDatabase;
        }
    }

}
