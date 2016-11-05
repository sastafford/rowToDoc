package com.marklogic.spring.batch.config;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.helper.DatabaseClientProvider;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.spring.batch.Options;
import com.marklogic.spring.batch.columnmap.ColumnMapSerializer;
import com.marklogic.spring.batch.columnmap.DefaultStaxColumnMapSerializer;
import com.marklogic.spring.batch.columnmap.JsonColumnMapSerializer;
import com.marklogic.spring.batch.item.PathAwareColumnMapProcessor;
import com.marklogic.spring.batch.config.support.OptionParserConfigurer;
import com.marklogic.spring.batch.item.ColumnMapItemWriter;
import joptsimple.OptionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a simple approach for migrating rows to documents via Spring JDBC ColumnMaps.
 */
@EnableBatchProcessing
@ComponentScan(basePackages = {"com.marklogic.spring.batch.columnmap", "com.marklogic.spring.batch.item"})
public class RowToDoc implements OptionParserConfigurer {

    @Autowired
    private Environment env;

    @Resource(name="jsonColumnMapSerializer")
    private ColumnMapSerializer jsonColumnMapSerializer;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource(name="columnMapItemWriter")
    private ColumnMapItemWriter writer;

    @Override
    public void configureOptionParser(OptionParser parser) {
        parser.accepts("sql", "The SQL query for selecting rows to migrate").withRequiredArg();
        parser.accepts("format", "The format of the documents written to MarkLogic - either xml or json").withRequiredArg().defaultsTo("xml");
        parser.accepts("rootLocalName", "Name of the root element in each document written to MarkLogic").withRequiredArg();
        parser.accepts("collections", "Comma-separated list of collections to add each document to").withRequiredArg();
    }

    @Bean
    public Job job(JobBuilderFactory jobBuilderFactory, @Qualifier("step1") Step step1) {
        return jobBuilderFactory.get("rowToDocConfig").start(step1).build();
    }

    @Bean
    public DatabaseClient databaseClient(DatabaseClientProvider databaseClientProvider) {
        return databaseClientProvider.getDatabaseClient();
    }

    public ItemReader<Map<String, Object>> reader(String sql) {
        JdbcCursorItemReader<Map<String, Object>> reader = new JdbcCursorItemReader<>();

        reader.setDataSource(buildDataSource());
        reader.setRowMapper(new ColumnMapRowMapper());
        reader.setSql(sql);

        return reader;
    }

    @Bean
    @JobScope
    public Step step1(
            StepBuilderFactory stepBuilderFactory,
            @Value("#{jobParameters['sql']}") String sql,
            @Value("#{jobParameters['format'] ?: 'xml'}") String format,
            @Value("#{jobParameters['root_local_name']}") String rootLocalName,
            @Value("#{jobParameters['collections']}") String[] collections,
            @Value("#{jobParameters['transform_name'] ?: ''}") String transformName,
            @Value("#{jobParameters['transform_parameters'] ?: ''}") String transformParameters) {

        DocumentMetadataHandle metadata = new DocumentMetadataHandle();
        if (collections == null || collections.length == 0) {
            String[] coll = {rootLocalName};
            metadata.withCollections(coll);
        } else {
            metadata.withCollections(collections);
            logger.debug("Setting collections to: " + String.join(",", collections));
        }

        if ("json".equals(format)) {
            writer.setColumnMapSerializer(jsonColumnMapSerializer);
            logger.debug("Setting output to json");
        }

        if (!rootLocalName.isEmpty()) {
            writer.setRootLocalName(rootLocalName);
            logger.debug("Setting root local name to :" + rootLocalName);
        }

        if (!transformName.isEmpty()) {
            Map<String, String> paramsMap = new HashMap<String, String>();
            if (!transformParameters.isEmpty()) {
                String params[] = transformParameters.split(",");
                for (int i = 0; i < params.length; i += 2) {
                    paramsMap.put(params[i], params[i + 1]);
                }
            }

            writer.setTransform(Format.valueOf(format.toUpperCase()), transformName, paramsMap);
            logger.debug(String.format("Setting transform to %s with parameters (%s)", transformName, transformParameters));
        }
        writer.setMetadata(metadata);

        return stepBuilderFactory.get("step1")
                .<Map<String, Object>, Map<String, Object>>chunk(10)
                .reader(reader(sql))
                .processor(new PathAwareColumnMapProcessor())
                .writer(writer)
                .build();
    }

    /**
     * Protected so that a different data source can be used.
     */
    protected DataSource buildDataSource() {
        //logger.info("Creating simple data source based on JDBC connection options");
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(env.getProperty(Options.JDBC_DRIVER));
        ds.setUrl(env.getProperty(Options.JDBC_URL));
        ds.setUsername(env.getProperty(Options.JDBC_USERNAME));
        ds.setPassword(env.getProperty(Options.JDBC_PASSWORD));
        return ds;
    }

}
