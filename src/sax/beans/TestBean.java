package sax.beans;

public class TestBean {
  private Integer id;

  private String username;

  private String password;

  private Byte status;

  private TestBean testBean;
  private TestBean testBean2;


  public TestBean() {

  }


  public TestBean(Integer id, String username, String password, Byte status, TestBean testBean, TestBean testBean2) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.status = status;
    this.testBean = testBean;
    this.testBean2 = testBean2;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Byte getStatus() {
    return status;
  }

  public void setStatus(Byte status) {
    this.status = status;
  }

  public TestBean getTestBean() {
    return testBean;
  }

  public void setTestBean(TestBean testBean) {
    this.testBean = testBean;
  }

  public TestBean getTestBean2() {
    return testBean2;
  }

  public void setTestBean2(TestBean testBean2) {
    this.testBean2 = testBean2;
  }

  @Override
  public String toString() {
    return "TestBean{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", status=" + status +
            ", testBean=" + testBean +
            ", testBean2=" + testBean2 +
            '}';
  }
}
