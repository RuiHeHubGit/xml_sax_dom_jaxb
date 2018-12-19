package dom;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Stack;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * @author ruihe
 */
public class XmlDomUtil {
    private static Logger log = Logger.getLogger("xmldom");


    /**
     * object to document of xml
     * @param obj
     * @return
     */
    public static Document objectToDocument(Object obj) throws TranslateException {
        if(obj == null || baseTypeParseMap.get(obj.getClass()) != null) {
            throw new IllegalArgumentException("Argument must is not null and is a simple object");
        }
        String rootName = obj.getClass().getSimpleName();
        Document document = getBaseXmlDocument(rootName);
        Field[] fields = obj.getClass().getDeclaredFields();
        Node node = document.getFirstChild();

        Stack<Field[]> fieldsStack = new Stack<>();
        Stack<Node> nodeStack = new Stack<>();
        fieldsStack.push(fields);
        nodeStack.push(node);

        while (!fieldsStack.isEmpty()) {
            node = nodeStack.pop();
            fields = fieldsStack.pop();
            for (Field f : fields) {
                if (baseTypeParseMap.get(f.getType()) != null) {
                    Object value = getFieldValue(obj, f);
                    if (value != null) {
                        Node newNode = document.createElement(f.getName());
                        newNode.appendChild(document.createTextNode(value.toString()));
                        node.appendChild(newNode);
                    }
                } else {
                    fieldsStack.push(fields);
                    fields = f.getType().getDeclaredFields();
                    nodeStack.push(node);
                    Node newNode = document.createElement(f.getName());
                    node.appendChild(newNode);
                    node = newNode;
                }
            }
        }
        document.getDocumentElement().normalize();
        return document;
    }

    /**
     * document of xml to object
     * @param document
     * @param clazz
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws TranslateException
     */
    public static  <T> T documentToObject(Document document, Class<T> clazz) throws TranslateException {
        return documentToObect(document, clazz, null);
    }

    /**
     * get document of xml
     * @param is
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static Document getXmlDocument(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        return doc;
    }

    /**
     * get document of base by rootName
     * @param rootName
     * @return
     */
    public static Document getBaseXmlDocument(String rootName) {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        ByteArrayInputStream input = null;
        try {
            builder = factory.newDocumentBuilder();
            StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append("<?xml version=\"1.0\"?> <"+rootName+"></"+rootName+">");
            input = new ByteArrayInputStream(
                    xmlStringBuilder.toString().getBytes("UTF-8"));
            Document doc = builder.parse(input);
            return doc;
        } catch (ParserConfigurationException e) {
            log.severe(e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            log.severe(e.getLocalizedMessage());
        } catch (IOException e) {
            log.severe(e.getLocalizedMessage());
        } catch (SAXException e) {
            log.severe(e.getLocalizedMessage());
        } finally {
            if(input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.warning(e.getLocalizedMessage());
                }
            }
        }
        return null;
    }


    /**
     * save document to file
     * @param document
     * @param file
     * @throws TransformerException
     */
    public static void saveDocument(Document document, File file) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        Result output = new StreamResult(file);
        Source input = new DOMSource(document);
        transformer.transform(input, output);
    }

    /**
     * document of xml to object， only supper simple object
     * @param document
     * @param clazz
     * @param parentNode
     * @return
     * @throws TranslateException
     */
    private static <T> T  documentToObect(Document document, Class<T> clazz, Node parentNode) throws TranslateException {
        T obj = null;
        try {
            obj = clazz.newInstance();
        } catch (Exception e) {
            throw new TranslateException("new new instance faild", e);
        }
        NodeList nodeList = null;
        if(parentNode != null) {
            nodeList = parentNode.getChildNodes();
        } else {
            nodeList = document.getDocumentElement().getChildNodes();
        }

        for (int i=0; i< nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if(node.hasChildNodes()) {
                try {
                    Field field = clazz.getDeclaredField(node.getNodeName());
                    NodeList childNodeList = node.getChildNodes();
                    if(childNodeList.getLength() > 1) {
                        setFieldValue(obj, field, documentToObect(document, field.getType(), node));
                    } else {
                        setFieldValue(obj, field, parserBaseTypeValueByClass(childNodeList.item(0).getNodeValue(), field.getType()));
                    }
                } catch (NoSuchFieldException e) {
                    log.severe(e.getLocalizedMessage());
                }
            }
        }
        return obj;
    }

    /**
     * set value as field
     * @param tag
     * @param field
     * @param value
     * @throws TranslateException
     */
    private static void setFieldValue(Object tag, Field field, Object value) throws TranslateException {
        if(tag == null || field == null) {
            throw new NullPointerException("setFieldValue");
        }
        try {
            String fName = field.getName();
            String mName = "set" + fName.substring(0, 1).toUpperCase() + fName.substring(1, fName.length());
            Method method = tag.getClass().getMethod(mName, field.getType());
            method.invoke(tag, value);
        } catch (Exception e) {
            throw new TranslateException("no such method of set as:"+field.getName(), e);
        }
    }

    /**
     * get value as field
     * @param tag
     * @param field
     * @throws TranslateException
     */
    private static Object getFieldValue(Object tag, Field field) throws TranslateException {
        if(tag == null || field == null) {
            throw new NullPointerException("getFieldValue");
        }
        try {
            String fName = field.getName();
            String mName = "get" + fName.substring(0, 1).toUpperCase() + fName.substring(1, fName.length());
            Method method = tag.getClass().getMethod(mName);
            return method.invoke(tag);
        } catch (Exception e) {
            throw new TranslateException("no such method of set as:"+field.getName(), e);
        }
    }

    /**
     * parse strings as object by the class
     * @param value
     * @param aClass
     * @return
     */
    private static Object parserBaseTypeValueByClass(String value, Class<?> aClass) {
        Function<String, Object> parser = baseTypeParseMap.get(aClass);
        if(parser != null) {
            return parser.apply(value);
        }
        return null;
    }

    static class TranslateException extends Exception{
        public TranslateException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    private static HashMap<Class, Function<String, Object>>baseTypeParseMap = new HashMap<>();
    private static SimpleDateFormat formDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
    /**
     * @since jdk1.8
     */
    static {
        baseTypeParseMap.put(String.class, String::trim);
        baseTypeParseMap.put(Integer.class, Integer::parseInt);
        baseTypeParseMap.put(Long.class, Long::parseLong);
        baseTypeParseMap.put(Short.class, Short::parseShort);
        baseTypeParseMap.put(Float.class, Float::parseFloat);
        baseTypeParseMap.put(Double.class, Double::parseDouble);
        baseTypeParseMap.put(Byte.class, Byte::parseByte);
        baseTypeParseMap.put(Boolean.class, Boolean::parseBoolean);

        baseTypeParseMap.put(Character.class, s -> {
            if (s.isEmpty())
                return null;
            return s.charAt(0);
        });

        baseTypeParseMap.put(Date.class, s -> {
            if (s.isEmpty())
                return null;
            try {
                return formDate.parse(s);
            } catch (ParseException e) {
                log.severe(e.getLocalizedMessage());
            }
            return null;
        });
        log.info("SaxUtil initialization complete.");
    }
}
