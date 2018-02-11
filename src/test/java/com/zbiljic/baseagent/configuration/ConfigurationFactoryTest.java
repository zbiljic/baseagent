package com.zbiljic.baseagent.configuration;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigurationFactoryTest {

  public interface MyConfig {

    @Config("test.enabled")
    Boolean isEnabled();

    @Config(value = "test.name", defaultValue = "My Name")
    String getName();

    @Config("test.collection")
    Collection<String> getCollection();

    @Config("test.integers")
    Collection<Integer> getIntegers();

    @Config("test.subconfig")
    SubConfig getSubConfig();
  }

  public interface SubConfig {

    @Config("possible")
    Boolean isPossible();
  }

  private static Properties properties;

  @BeforeClass
  public static void setUp() throws Exception {
    properties = new Properties();
    properties.setProperty("test.collection", "col1,col2");
    properties.setProperty("test.integers", "2,3,5,7,11,13,17,19,23,29");
    properties.setProperty("test.subconfig.possible", "true");
  }

  @Test
  public void testIsEnabled() throws Exception {
    MyConfig myConfig = ConfigurationFactory.create(properties).bind(MyConfig.class);

    assertNotNull(myConfig);
    assertFalse(myConfig.isEnabled());
  }

  @Test
  public void testGetName() throws Exception {
    MyConfig myConfig = ConfigurationFactory.create(properties).bind(MyConfig.class);

    assertNotNull(myConfig);
    assertNotNull(myConfig.getName());
    assertEquals("My Name", myConfig.getName());
  }

  @Test
  public void testGetCollection() throws Exception {
    MyConfig myConfig = ConfigurationFactory.create(properties).bind(MyConfig.class);

    assertNotNull(myConfig);
    assertNotNull(myConfig.getCollection());
    assertEquals(2, myConfig.getCollection().size());
  }

  @Test
  public void testGetIntegers() throws Exception {
    MyConfig myConfig = ConfigurationFactory.create(properties).bind(MyConfig.class);

    assertNotNull(myConfig);
    assertNotNull(myConfig.getIntegers());
    assertEquals(10, myConfig.getIntegers().size());
  }

  @Test
  public void testToString() throws Exception {
    MyConfig myConfig = ConfigurationFactory.create(properties).bind(MyConfig.class);

    assertNotNull(myConfig);
    assertNotNull(myConfig.toString());
    assertTrue(myConfig.toString().startsWith(MyConfig.class.getSimpleName() + "{"));
  }
}
