package sax.beans;

import java.util.Date;
import java.util.List;

public class Order {
  private Integer id;
  private String name;
  private Integer userId;
  private Integer money;
  private Byte payWay;
  private Byte status;
  private String desc;
  private Date create;
  private String orderNumber;
  private List <Commodity> commodities;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Integer getMoney() {
    return money;
  }

  public void setMoney(Integer money) {
    this.money = money;
  }

  public Byte getPayWay() {
    return payWay;
  }

  public void setPayWay(Byte payWay) {
    this.payWay = payWay;
  }

  public Byte getStatus() {
    return status;
  }

  public void setStatus(Byte status) {
    this.status = status;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public Date getCreate() {
    return create;
  }

  public void setCreate(Date create) {
    this.create = create;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public List<Commodity> getCommodities() {
    return commodities;
  }

  public void setCommodities(List<Commodity> commodities) {
    this.commodities = commodities;
  }

  @Override
  public String toString() {
    return "Order{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", userId=" + userId +
        ", money=" + money +
        ", payWay=" + payWay +
        ", status=" + status +
        ", desc='" + desc + '\'' +
        ", create=" + create +
        ", orderNumber='" + orderNumber + '\'' +
        ", commodities=" + commodities +
        '}';
  }
}
