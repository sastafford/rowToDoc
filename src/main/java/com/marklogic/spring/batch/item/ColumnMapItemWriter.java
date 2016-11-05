package com.marklogic.spring.batch.item;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.GenericDocumentManager;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.spring.batch.columnmap.ColumnMapMerger;
import com.marklogic.spring.batch.columnmap.ColumnMapSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Features this can provide:
 * <ol>
 * <li>Assume the first column is the ID column, but provide a property to allow for a column name.</li>
 * <li>Provide a strategy interface for generating XML element names based on column names.</li>
 * </ol>
 */
@Component("columnMapItemWriter")
public class ColumnMapItemWriter implements ItemWriter<Map<String, Object>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    //
    // Where possible use read-only elements so we can run this in a
    // threaded environment like an async executor.
    //
    private String rootElementName;
    private ServerTransform serverTransform;
    private Format format;
    private boolean transformOn = false;
    private DocumentMetadataHandle metadata;

    @Autowired
    @Qualifier("staxColumnMapSerializer")
    private ColumnMapSerializer columnMapSerializer;

    @Autowired
    private DatabaseClient databaseClient;

    public void setMetadata(DocumentMetadataHandle metadata) {
        this.metadata = metadata;
    }

    // Internal state
    private GenericDocumentManager mgr;

    @PostConstruct
    public void postConstruct() {
        this.mgr = databaseClient.newDocumentManager();
        this.rootElementName = "item";
    }

    /**
     * So what we need to do is, given an ID, we need to see if there's already a Map for that ID. If there is, we need
     * to merge the data from the new item into the existing item.
     * 
     * When we get a column label like address/street, we need to tokenize it...
     */
    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        DocumentWriteSet set = mgr.newWriteSet();

        for (Map<String, Object> columnMap : items) {
            try {
                logger.debug("Writing record: " + columnMap);
                String content = columnMapSerializer.serializeColumnMap(columnMap, this.rootElementName, null);
                String uri = generateUri(content);

                set.add(uri, metadata, new StringHandle(content));
                logger.debug("Writing URI: " + uri + "; content: " + content);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            if (!set.isEmpty()) {
                logger.debug("Writing set of documents");
                if (!transformOn) {
                    mgr.write(set);
                } else {
                    mgr.write(set, serverTransform);
                }
                logger.debug("Finished writing set of documents");
            }

        }
    }

    /**
     * Does some hacking to determine if we need a json or xml suffix.
     *
     * TODO UriGenerator needs more support; it needs to be smart enough to determine the suffix
     * itself.
     *
     * @param content
     * @return
     */
    protected String generateUri(String content) {
        String uri = String.format("/%s/%s", this.rootElementName, UUID.randomUUID().toString());
        if (content.startsWith("{")) {
            return uri += ".json";
        } else if (content.startsWith("<")) {
            return uri += ".xml";
        }
        return uri;
    }

    public void setTransform(Format format, String transformName, Map<String, String> transformParameters) {
        this.format = format;
        mgr.setContentFormat(format);
        this.serverTransform = new ServerTransform(transformName);
        if (transformParameters != null) {
            for (String key : transformParameters.keySet()) {
                serverTransform.put(key, transformParameters.get(key));
            }
        }
        transformOn = true;
    }

    public void setColumnMapSerializer(ColumnMapSerializer columnMapSerializer) {
        this.columnMapSerializer = columnMapSerializer;
    }

    public void setDatabaseClient(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public void setRootLocalName(String rootLocalName) {
        this.rootElementName = rootLocalName;
    }
}