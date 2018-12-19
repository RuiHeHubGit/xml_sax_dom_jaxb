import dom.XmlDomTester;
import sax.SaxTester;

public class Main {

  public static void main(String[] args) {
    SaxTester saxTester = new SaxTester();
  //  saxTester.test();

    XmlDomTester domTester = new XmlDomTester();
    domTester.test();
  }
}
