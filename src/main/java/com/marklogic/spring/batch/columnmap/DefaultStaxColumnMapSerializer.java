package com.marklogic.spring.batch.columnmap;

import com.marklogic.client.helper.LoggingObject;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Default implementation that uses the Stax library to create an XML structure. Any XML library, or even a templating
 * engine like Velocity/Freemarker, could be used here instead.
 */
@Service("staxColumnMapSerializer")
public class DefaultStaxColumnMapSerializer extends LoggingObject implements ColumnMapSerializer {

    private XMLOutputFactory xmlOutputFactory;

    public DefaultStaxColumnMapSerializer() {
        this.xmlOutputFactory = XMLOutputFactory.newFactory();
    }

    @Override
    public String serializeColumnMap(Map<String, Object> columnMap, String rootLocalName, String rootNamespaceUri) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter sw = xmlOutputFactory.createXMLStreamWriter(out);
            sw.writeStartElement(rootLocalName);
            writeColumnMap(columnMap, sw);
            sw.writeEndElement();
            sw.flush();
        } catch (XMLStreamException ex) {
            throw new RuntimeException(ex);
        }
        return out.toString();
    }

    /**
     * Recursive function that will call itself when it finds that the value of a key/value pair in the column map is a
     * Map<String, Object> itself.
     * 
     * @param columnMap the column map
     * @param sw the stream writer
     * @throws XMLStreamException when it fails
     */
    private void writeColumnMap(Map<String, Object> columnMap, XMLStreamWriter sw) throws XMLStreamException {
        for (String key : columnMap.keySet()) {
            Object value = columnMap.get(key);
            if (value != null) {
                if (value instanceof List) {
                    List list = (List) value;
                    for (Object item : list) {
                        if (item != null) {
                            sw.writeStartElement(key);
                            if (item instanceof Map) {
                                writeColumnMap((Map<String, Object>) item, sw);
                            } else {
                                sw.writeCharacters(item.toString());
                            }
                            sw.writeEndElement();
                        }
                    }
                } else if (value instanceof Map) {
                    sw.writeStartElement(key);
                    writeColumnMap((Map<String, Object>) value, sw);
                    sw.writeEndElement();
                } else if (value instanceof byte[]) {
                    // TODO Figure out what to do with blobs by default
                    logger.info("Ignoring blob, key: " + key);
                } else {
                    String text = value.toString();
                    if (text != null && text.trim().length() > 0) {
                        sw.writeStartElement(key);
                        sw.writeCharacters(text);
                        sw.writeEndElement();
                    }
                }
            }
        }
    }

}
