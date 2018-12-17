package sax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.SAXException;
import sax.SaxUtil.TranslateXmlException;
import sax.beans.Commodity;
import sax.beans.Order;
import sax.beans.TestBean;

public class SaxTester {
  public void test() {
    try {
      TestBean testBean1 = new TestBean(1, "test1", "password1", null,
              new TestBean(2, "test2", "password2", null, null, null),
              new TestBean(3, "test3", "password3", null, null,
                      new TestBean(4, "test4", "password4", null, null, null)));


      System.out.println(testBean1);
      String xml = SaxUtil.childToXml(testBean1);
      System.out.println(xml);
      TestBean parseTestBean = SaxUtil.parse(xml, TestBean.class);
      System.out.println(parseTestBean);

      Order order = new Order();
      order.setId(1);
      order.setCreate(new Date());
      order.setDesc("order test");
      order.setName("order");
      order.setMoney(1000000);
      order.setStatus(Integer.valueOf(1).byteValue());
      order.setUserId(1);
      order.setOrderNumber(UUID.randomUUID().toString().replace("-", ""));

      List<Commodity> commodities = new ArrayList<>();
      order.setCommodities(commodities);
      Commodity commodity = new Commodity();
      commodities.add(commodity);
      commodity.setId(1);
      commodity.setName("cname");
      commodity.setDesc("test");
      commodity.setNumber("bk000001");
      commodity.setPrice(5300);
      HashMap attr = new HashMap();
      commodity.setAttributes(attr);
      attr.put(null, null);
      attr.put("w", "50cm");
      attr.put("h", "20cm");
      attr.put("l", "20cm");
      System.out.println(order);
      xml = SaxUtil.childToXml(order);
      System.out.println(xml);
      order = SaxUtil.parse(xml, Order.class);
      System.out.println(order);
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TranslateXmlException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
  }
}
