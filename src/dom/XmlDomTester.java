package dom;

import dom.beans.Student;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XmlDomTester {
    public void test() {
        InputStream is = null;
        try {
            is = new FileInputStream(new File("class.xml"));
            Document document = XmlDomUtil.getXmlDocument(is);
            printDocument(document, null);
            Student student = XmlDomUtil.documentToObject(document, Student.class);
            System.out.println(student);
            Document document1 = XmlDomUtil.objectToDocument(student);
            printDocument(document1, null);
            student = XmlDomUtil.documentToObject(document1, Student.class);
            System.out.println(student);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (XmlDomUtil.TranslateException e) {
            e.printStackTrace();
        } finally {
            if(is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void printDocument(Document document, Node parentNode) {
        NodeList nodeList = null;
        if(parentNode != null) {
            nodeList = parentNode.getChildNodes();
        } else {
            nodeList = document.getDocumentElement().getChildNodes();
        }

        for (int i=0; i< nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if(node.hasChildNodes()) {
                System.out.print("\n"+node.getNodeName()+":");
                printDocument(document, node);
            } else {
                System.out.print(node.getNodeValue());
            }
        }
    }
}
