package sax;

import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl;
import com.sun.xml.internal.stream.writers.XMLStreamWriterImpl;
import com.sun.xml.internal.ws.util.xml.StAXResult;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Logger;

import static com.sun.xml.internal.stream.writers.XMLStreamWriterImpl.UTF_8;

/**
 * @author ruihe
 */
public class SaxUtil {
  private static SAXParserFactoryImpl factory = new SAXParserFactoryImpl();
  private static HashMap<Class<?>, Function<String,Object>> baseTypeParseMap = new HashMap<>();
  private static SimpleDateFormat formDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
  private static Logger log = Logger.getLogger("sax");

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
        log.severe(e.getLocalizedMessage());
      }
      return null;
    });
    log.info("SaxUtil initialization complete.");
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
          throws SAXException {
    if(xml == null || xml.isEmpty() || tClass == null) {
      throw new IllegalArgumentException();
    }
    Entry<String, T> instance = new SimpleEntry<String, T>("object", null);
    SAXParser parser = null;
    try {
      parser = factory.newSAXParser();
    } catch (ParserConfigurationException e) {
      log.severe(e.getLocalizedMessage());
      throw new SAXException(e);
    }
    ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());
    try {
      parser.parse(inputStream, new DefaultParseHandler(tClass, instance));
    } catch (IOException e) {
      log.severe(e.getLocalizedMessage());
      throw new SAXException(e);
    }
    return instance.getValue();
  }

  /**
   * object translate into xml not outer label
   * @param obj
   * @return
   * @throws TransformerConfigurationException
   * @throws IOException
   */
  public static String objectToXml(final Object obj)
          throws TranslateXmlException {

    SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    TransformerHandler handler = null;
    try {
      handler = factory.newTransformerHandler();
    } catch (TransformerConfigurationException e) {
      log.severe(e.getLocalizedMessage());
      throw new TranslateXmlException(e);
    }
    Transformer transformer = handler.getTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, UTF_8);
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PropertyManager propertyManager = new PropertyManager(PropertyManager.CONTEXT_WRITER);
    XMLStreamWriter writer = null;
    try {
      writer = new XMLStreamWriterImpl(outputStream, propertyManager);
    } catch (IOException e) {
      log.severe(e.getLocalizedMessage());
      throw new TranslateXmlException(e);
    }
    Result result = new StAXResult(writer);
    handler.setResult(result);

    try {
      handler.startDocument();
      handler.startElement("", "", obj.getClass().getSimpleName(), createAttributes(obj));
    } catch (SAXException e) {
      log.severe(e.getLocalizedMessage());
      throw new TranslateXmlException(e);
    }
    childToInnerXml(obj, handler);
    try {
      handler.endElement("", "", obj.getClass().getSimpleName());
      handler.endDocument();
    } catch (SAXException e) {
      log.severe(e.getLocalizedMessage());
      throw new TranslateXmlException(e);
    }
    return new String(outputStream.toByteArray());
  }

  /**
   * object translate into xml not outer label
   * @param obj
   * @param handler
   * @throws TranslateXmlException
   */
  private static void childToInnerXml(Object obj, TransformerHandler handler)
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

        if(r == null) {
          continue;
        }

        handler.startElement("", "", fName, createAttributes(r));
        if(Map.class.isAssignableFrom(f.getType())) {
          mapToXml((Map) r, handler);
        } else if(Collection.class.isAssignableFrom(f.getType())) {
          collectionToXml((Collection) r, handler);
        } else if(!baseTypeParseMap.containsKey(f.getType())) {
          childToInnerXml(r, handler);
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
      try {
        handler.startElement("", "", item.getClass().getSimpleName(), null);
        childToInnerXml(item, handler);
        handler.endElement("", "", item.getClass().getSimpleName());
      } catch (SAXException e) {
        throw new TranslateXmlException(e);
      }
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
          childToInnerXml(value, handler);
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

  private static Object parserBaseTypeValueByClass(String value, Class<?> aClass) {
    Function<String, Object> parser = baseTypeParseMap.get(aClass);
    if(parser != null) {
      return parser.apply(value);
    }
    return null;
  }

  private static Class getBaseTypeTypeName(String typeName) {
    for (Class clazz : baseTypeParseMap.keySet()) {
      if(clazz.getSimpleName().equals(typeName)) {
        return clazz;
      }
    }
    return null;
  }

  static class DefaultParseHandler extends DefaultHandler {
    private Entry<String, Object> rootInstance;
    private int xmlPathLength;
    private int rootObjPathLength;
    private Class<?> rootClass;
    private Class<?> currentClass;
    private Class<?> collectionTypeClass;
    private Class<?>[] mapTypeClasses;
    private Field currentField;
    private Object currentObject;
    private HashMap<Class<?>, Field[]> classFieldsMap;
    private HashMap<Integer, Object> objectsMap;
    private String currentQName;
    private String elementType;
    private Object lastMapKey;
    private boolean isCollection;
    private boolean isMap;
    private boolean mayBeMap;


    public DefaultParseHandler(Class<?> rootClass, Entry rootInstance) {
      this.rootClass = rootClass;
      this.rootInstance = rootInstance;
    }

    @Override
    public void startDocument() throws SAXException {
      super.startDocument();
      classFieldsMap = new HashMap();
      objectsMap = new HashMap<>();
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
      currentQName = qName;
      elementType = attributes.getValue("type");

      if(!mayBeMap && xmlPathLength - rootObjPathLength > 1) {
        return;
      }

      Class fieldClass = null;

      if(rootInstance.getValue() == null) {
        fieldClass = rootClass;
      } else if(mayBeMap) {
        mayBeMap = false;
        createFieldObjectAndInitInfo(HashMap.class, null, lastMapKey);
      } else if(isCollection) {
        isCollection = false;
        fieldClass = collectionTypeClass;
        currentField = null;
      } else if(isMap) {
        isMap = false;
        fieldClass = mapTypeClasses[1];
        mapTypeClasses[1] = null;
        if(fieldClass == null) {
          fieldClass = getBaseTypeTypeName(elementType);
        }
        if(fieldClass == null) {
          mayBeMap = true;
          lastMapKey = getMapKey(HashMap.class, attributes, qName);
        }
        currentField = null;
      } else {
        currentField = checkCurrentField(currentClass, qName);
        if(currentField != null) {
          fieldClass = currentField.getType();
        } else if(elementType != null){
          fieldClass = getBaseTypeTypeName(elementType);
        } else {
          if(fieldClass == null) {
            mayBeMap = true;
            lastMapKey = getMapKey(HashMap.class, attributes, qName);
          }
        }
      }

      if(fieldClass != null && (baseTypeParseMap.get(fieldClass) == null || Map.class.isAssignableFrom(fieldClass))) {
        createFieldObjectAndInitInfo(fieldClass, attributes, null);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      super.endElement(uri, localName, qName);
      --xmlPathLength;
      currentQName = "";
      elementType = "";
      lastMapKey = null;
      mayBeMap = false;

      if(rootObjPathLength == xmlPathLength) {
        isCollection = false;
        isMap = false;
      }

      Object parent = objectsMap.get(xmlPathLength - 1);
      if(parent != null) {
        if(Collection.class.isAssignableFrom(parent.getClass())) {
          isCollection = true;
        }
      }

      if(rootObjPathLength > xmlPathLength) {
        rootObjPathLength = xmlPathLength;
        currentObject = parent;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      String value = new String(ch, start, length);
      try {
        if(Map.class.isAssignableFrom(currentObject.getClass())) {
          putMapKeyValue(currentObject, currentQName, parserBaseTypeValueByClass(value, getBaseTypeTypeName(elementType)));
        } else {
          setObjectFieldValue(currentObject, currentField, parserBaseTypeValueByClass(value, currentField.getType()));
        }
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        log.severe(e.getLocalizedMessage());
        throw new SAXException(e);
      }
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
      super.error(e);
      log.finest(e.getLocalizedMessage());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
      super.fatalError(e);
      log.finest(e.getLocalizedMessage());
    }

    /*
     * 获取field的泛型类型
     * field
     */
    public Class[] getActualTypeArguments(Field field) throws Exception{
      Class[] classes = new Class[0];
      if(field == null) {
        return classes;
      }
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
        if(Collection.class.isAssignableFrom(tag.getClass())) {
          ((Collection)tag).add(value);
        } else if(field != null) {
          String fName = field.getName();
          String mName = "set" + fName.substring(0, 1).toUpperCase() + fName.substring(1, fName.length());
          Method method = tag.getClass().getMethod(mName, field.getType());
          method.invoke(tag, value);
        }
      }
    }

    private Object getMapKey(Class<?> keyTypeClass, Attributes attributes, String qName) throws SAXException{
      if(keyTypeClass == null || attributes.getLength() == 0 || String.class.isAssignableFrom(keyTypeClass)) {
        return qName;
      } else if(Object.class.equals(keyTypeClass)) {
        try {
          return parseElementAttributes(HashMap.class, attributes);
        } catch (Exception e) {
          log.severe(e.getLocalizedMessage());
          throw new SAXException(e);
        }
      } else {
        try {
          return parseElementAttributes(keyTypeClass, attributes);
        } catch (Exception e) {
          log.severe(e.getLocalizedMessage());
          throw new SAXException(e);
        }
      }
    }

    private Object parseElementAttributes(Class<?> keyTypeClass, Attributes attributes) throws IllegalAccessException, InstantiationException {
      if(keyTypeClass == null || attributes == null) {
        return null;
      }
      Object tag = keyTypeClass.newInstance();
      if(Map.class.isAssignableFrom(keyTypeClass)) {
        for (int i=0; i<attributes.getLength(); ++i) {
          ((Map)tag).put(attributes.getQName(i), attributes.getValue(i));
        }
      }
      Field[] fields = tag.getClass().getDeclaredFields();
      for (Field f : fields) {
        for (int i=0; i<attributes.getLength(); ++i) {
          if(f.getName().equals(attributes.getQName(i))) {
            try {
              setObjectFieldValue(tag, f, parserBaseTypeValueByClass(attributes.getValue(i), f.getType()));
            } catch (Exception e) {
              log.severe(e.getLocalizedMessage());
            }
            break;
          }
        }
      }
      return tag;
    }

    private void putMapKeyValue(Object map, Object key, Object value) {
      if(map == null || !Map.class.isAssignableFrom(map.getClass())) {
        throw new IllegalArgumentException();
      }
      ((Map)map).put(key, value);
    }

    private void createFieldObjectAndInitInfo(Class fieldClass, Attributes attributes, Object lastQName) throws SAXException {
      if(Object.class.equals(fieldClass)) {
        fieldClass = HashMap.class;
      }
      currentClass = fieldClass;
      try {
        Object parent = objectsMap.get(rootObjPathLength-1);
        currentObject = createInstance(fieldClass);
        objectsMap.put(rootObjPathLength, currentObject);
        ++rootObjPathLength;
        if(parent != null) {
          if(Map.class.isAssignableFrom(parent.getClass())) {
            Object key = lastQName;
            if(key == null) {
              key = getMapKey(mapTypeClasses[0], attributes, currentQName);
            }
            putMapKeyValue(parent, key, currentObject);
          } else {
            setObjectFieldValue(parent, currentField, currentObject);
          }
        }

        if(rootInstance.getValue() == null) {
          rootInstance.setValue(currentObject);
        }
      } catch (Exception e) {
        log.severe(e.getLocalizedMessage());
        throw new SAXException(e);
      }

      if(Collection.class.isAssignableFrom(currentClass)) {
        try {
          Class[] classes = getActualTypeArguments(currentField);
          if(classes.length == 1) {
            collectionTypeClass = classes[0];
          } else {
            collectionTypeClass = HashMap.class;
          }
        } catch (Exception e) {
          log.severe(e.getLocalizedMessage());
          throw new SAXException(e);
        }
        isCollection = true;
      } else if(Map.class.isAssignableFrom(currentClass)) {
        try {
          Class[] classes = getActualTypeArguments(currentField);
          if(classes.length == 2) {
            if(mapTypeClasses == null) {
              mapTypeClasses = new Class[2];
            }
            mapTypeClasses[0] = classes[0];
            mapTypeClasses[1] = classes[1];
          } else {
            mapTypeClasses[0] = HashMap.class;
            mapTypeClasses[1] = null;
          }
        } catch (Exception e) {
          log.severe(e.getLocalizedMessage());
          throw new SAXException(e);
        }
        isMap = true;
      } else {
        classFieldsMap.put(fieldClass, fieldClass.getDeclaredFields());
      }
    }

    private Field checkCurrentField(Class<?> currentClass, String fieldName) {
      Field[] fields = classFieldsMap.get(currentClass);
      if(fields != null) {
        for (Field field : fields) {
          if (field.getName().equals(fieldName)) {
            return field;
          }
        }
      }
      return null;
    }

  }

  static class TranslateXmlException extends Exception {
    public TranslateXmlException(Exception e) {
      super(e);
    }
  }
}
