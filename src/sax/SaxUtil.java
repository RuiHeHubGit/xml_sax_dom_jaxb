package sax;

import static com.sun.xml.internal.stream.writers.XMLStreamWriterImpl.UTF_8;

import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import com.sun.xml.internal.stream.writers.XMLStreamWriterImpl;
import com.sun.xml.internal.ws.util.xml.StAXResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author ruihe
 */
public class SaxUtil {
  private static SAXParserFactoryImpl factory = new SAXParserFactoryImpl();
  private static HashMap<Class<?>, Function<String,Object>> baseTypeParseMap = new HashMap<>();
  private static SimpleDateFormat formDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);

  /**
   * @since 1.8
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
        e.printStackTrace();
      }
      return null;
    });
  }

  /**
   * xml parse to object
   * @param xml
   * @param tClass
   * @param <T>
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  public static <T> T parse(String xml, Class<T> tClass)
      throws ParserConfigurationException, SAXException, IOException {
    if(xml == null || xml.isEmpty() || tClass == null) {
      throw new IllegalArgumentException();
    }
    Entry<String, T> instance = new SimpleEntry<String, T>("object", null);
    SAXParser parser = factory.newSAXParser();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());
    parser.parse(inputStream, new DefaultParseHandler(tClass, instance));
    return instance.getValue();
  }

  /**
   * object translate into xml
   * @param obj
   * @return
   * @throws TransformerConfigurationException
   * @throws IOException
   */
  public static String childToXml(final Object obj)
      throws TransformerConfigurationException, IOException, TranslateXmlException {

    SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    TransformerHandler handler = factory.newTransformerHandler();
    Transformer transformer = handler.getTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, UTF_8);
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PropertyManager propertyManager = new PropertyManager(PropertyManager.CONTEXT_WRITER);
    XMLStreamWriter writer = new XMLStreamWriterImpl(outputStream, propertyManager);
    Result result = new StAXResult(writer);
    handler.setResult(result);

    try {
      handler.startDocument();
      handler.startElement("", "", obj.getClass().getSimpleName(), createAttributes(obj));
    } catch (SAXException e) {
      e.printStackTrace();
    }
    childToXml(obj, handler);
    try {
      handler.endElement("", "", obj.getClass().getSimpleName());
      handler.endDocument();
    } catch (SAXException e) {
      e.printStackTrace();
    }
    return new String(outputStream.toByteArray());
  }

  private static void childToXml(Object obj, TransformerHandler handler)
      throws TranslateXmlException {
    if(obj == null) {
      return;
    }

    Class oClass = obj.getClass();
    Field[] fields = oClass.getDeclaredFields();
    for (Field f : fields) {
      try {
        String fName = f.getName();
        String mName = "get"+fName.substring(0, 1).toUpperCase()+fName.substring(1, fName.length());
        Method method = oClass.getMethod(mName);
        Object r = method.invoke(obj);

        handler.startElement("", "", fName, createAttributes(r));
        if(Map.class.isAssignableFrom(f.getType())) {
          mapToXml((Map) r, handler);
        } else if(Collection.class.isAssignableFrom(f.getType())) {
          collectionToXml((Collection) r, handler);
        } else if(!baseTypeParseMap.containsKey(f.getType())) {
          childToXml(r, handler);
        } else {
          if(r != null) {
            char[] chars = r.toString().toCharArray();
            handler.characters(chars, 0, chars.length);
          }
        }
        handler.endElement("", "", fName);
      } catch (NoSuchMethodException | IllegalAccessException | SAXException | InvocationTargetException e) {
        throw new TranslateXmlException(e);
      }
    }
  }

  private static void collectionToXml(Collection collection, TransformerHandler handler)
      throws TranslateXmlException {
    if(collection == null) {
      return;
    }
    for (Object item : collection) {
      childToXml(item, handler);
    }
  }

  private static void mapToXml(Map map, TransformerHandler handler) throws TranslateXmlException {
    if(map == null) {
      return;
    }
    Set keySet = map.keySet();
    for (Object key : keySet) {
      try {
        String elementName = null;
        if(key == null) {
          elementName = "null";
        } else if(key.getClass() == String.class) {
          elementName = (String) key;
        } else {
          elementName = key.getClass().getSimpleName();
        }

        handler.startElement("", "", elementName, createAttributes(key));

        Object value = map.get(key);
        if(value == null) {
          handler.characters("null".toCharArray(), 0, 4);
        } else if(baseTypeParseMap.containsKey(value.getClass())) {
          char[] chars = value.toString().toCharArray();
          handler.characters(chars, 0, chars.length);
        } else {
          childToXml(value, handler);
        }

        handler.endElement("", "", elementName);

      } catch (Exception e) {
        throw new TranslateXmlException(e);
      }
    }
  }

  private static Attributes createAttributes(Object obj) {
    if(obj == null) {
      return null;
    }
    Attributes attributes = null;
    Class oClass = obj.getClass();
    String typeName = null;
    if(Collection.class.isAssignableFrom(oClass)) {
      typeName = "collection";
    } else if(Map.class.isAssignableFrom(oClass)) {
      typeName = "map";
    }

    if(typeName == null && baseTypeParseMap.containsKey(oClass)) {
      typeName = oClass.getSimpleName();
    }

    if(typeName != null) {
      attributes = new AttributesImpl();
      ((AttributesImpl) attributes).addAttribute("", "type", "", "String", typeName);
    }
    return attributes;
  }

  private static Object parserBaseTypeValue(String value, Class<?> aClass) {
    Function<String, Object> parser = baseTypeParseMap.get(aClass);
    if(parser != null) {
      return parser.apply(value);
    }
    return null;
  }

  static class DefaultParseHandler extends DefaultHandler {
    private Entry<String, Object> rootInstance;
    private int xmlPathLength;
    private int rootObjPathLength;
    private Class<?> rootClass;
    private Class<?> currentClass;
    private Field currentField;
    private Object currentObject;
    private HashMap<Class<?>, Field[]> classFieldsMap;
    private HashMap<Integer, Object> objectsMap;
    private HashMap<Integer, Integer> findFieldCountMap;
    private String currentQName;
    private String currentFieldType;


    public DefaultParseHandler(Class<?> rootClass, Entry rootInstance) {
      this.rootClass = rootClass;
      this.rootInstance = rootInstance;
    }

    @Override
    public void startDocument() throws SAXException {
      super.startDocument();
      classFieldsMap = new HashMap();
      objectsMap = new HashMap<>();
      findFieldCountMap = new HashMap<>();
      currentClass = rootClass;
    }

    @Override
    public void endDocument() throws SAXException {
      super.endDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      super.startElement(uri, localName, qName, attributes);
      ++xmlPathLength;

      if(xmlPathLength - rootObjPathLength > 1) {
        return;
      }

      currentQName = qName;
      currentFieldType = attributes.getValue("type");

      Field[] fields = classFieldsMap.get(currentClass);
      if(fields != null) {
        for (Field field : fields) {
          if (field.getName().equals(qName)) {
            currentField = field;
            findFieldCountMap.put(rootObjPathLength, findFieldCountMap.get(rootObjPathLength)+1);
            break;
          }
        }
      }

      Class fieldClass = null;
      if(currentField != null) {
        fieldClass = currentField.getType();
      }

      if(rootInstance.getValue() == null) {
        fieldClass = rootClass;
      }

      if(fieldClass != null && baseTypeParseMap.get(fieldClass) == null) {
        currentClass = fieldClass;
        try {
          Object parent = currentObject;
          currentObject = createInstance(fieldClass);
          objectsMap.put(xmlPathLength, currentObject);

          if(parent != null) {
            setObjectFieldValue(parent, currentField, currentObject);
          }

          if(rootInstance.getValue() == null) {
            rootInstance.setValue(currentObject);
          }
        } catch (Exception e) {
          throw new SAXException(e);
        }

        if (!Collection.class.isAssignableFrom(fieldClass) && !Map.class.isAssignableFrom(fieldClass)) {
          classFieldsMap.put(fieldClass, fieldClass.getDeclaredFields());
        }
        ++rootObjPathLength;
        findFieldCountMap.put(rootObjPathLength, 0);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      super.endElement(uri, localName, qName);
      --xmlPathLength;
      currentQName = "";
      currentFieldType = "";

      if(rootObjPathLength > xmlPathLength) {
        // set field to be null when field properties are all empty.
        if((rootObjPathLength < findFieldCountMap.size() || rootObjPathLength > 1) && findFieldCountMap.get(rootObjPathLength) == 0) {
          try {
            setObjectFieldValue(objectsMap.get(rootObjPathLength-1), currentField, null);
          } catch (Exception e) {
            throw new SAXException(e);
          }
        }

        rootObjPathLength = xmlPathLength;
        currentObject = objectsMap.get(rootObjPathLength);
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      String value = new String(ch, start, length);
      try {
        setObjectFieldValue(currentObject, currentField, parserBaseTypeValue(value, currentField.getType()));
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        throw new SAXException(e);
      }
    }

    private Object createInstance(Class<?> type)
        throws IllegalAccessException, InstantiationException {
      if(Map.class.isAssignableFrom(type)) {
        return new HashMap<>();
      } else if(Collection.class.isAssignableFrom(type)) {
        return new ArrayList<>();
      }
      return type.newInstance();
    }

    private void setObjectFieldValue(Object tag, Field field, Object value)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      if(tag != null) {
        String fName = field.getName();
        String mName = "set"+fName.substring(0, 1).toUpperCase()+fName.substring(1, fName.length());
        Method method = tag.getClass().getMethod(mName, field.getType());
        method.invoke(tag, value);
      }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      super.error(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      super.fatalError(e);
    }

    /*
     * 获取field的泛型类型
     * field
     */
    public static Class[] getActualTypeArguments(Field field) throws Exception{
      Class[] classes = new Class[0];
      Type fieldGenericType = field.getGenericType();
      if(fieldGenericType instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        classes = new Class[typeArguments.length];
        for (int i = 0; i < typeArguments.length; i++) {
          classes[i] = Class.forName(typeArguments[i].getTypeName());
        }
      }
      return classes;
    }
  }

  static class TranslateXmlException extends Exception {
    public TranslateXmlException(Exception e) {
      super(e);
    }
  }
}
