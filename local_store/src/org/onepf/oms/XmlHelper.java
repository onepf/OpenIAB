package org.onepf.oms;

import android.content.res.AssetManager;
import android.util.Log;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XmlHelper {
    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try {
            doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (SAXException e) {
            Log.i("XmlHelper", "Parse error.", e);
            doc = null;
        } catch (NullPointerException e) {
            Log.i("XmlHelper", "Parse error. String must not be null.", e);
            doc = null;
        }
        return doc;
    }
}
