package com.zbiljic.baseagent.configuration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Properties;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A factory class to instantiate {@link Config} instances.
 */
public final class ConfigurationFactory {

  public static ConfigurationFactory create() {
    return create(new Properties());
  }

  public static ConfigurationFactory create(Properties properties) {
    return new ConfigurationFactory(properties);
  }

  private final Properties properties;

  private ConfigurationFactory(Properties properties) {
    this.properties = properties;
  }

  public ConfigurationFactory addSystemProperties() {
    Properties p = new Properties();
    p.putAll(this.properties);
    p.putAll(System.getProperties());
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory addSystemEnv() {
    Properties p = new Properties();
    p.putAll(this.properties);
    p.putAll(System.getenv());
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory addProperties(Properties properties) {
    Properties p = new Properties();
    p.putAll(this.properties);
    p.putAll(properties);
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory addPropertiesFromResource(String name) {
    Properties p = new Properties();
    p.putAll(this.properties);
    try {
      InputStream resource = ConfigurationFactory.class.getResourceAsStream(name);
      if (resource != null) {
        p.load(resource);
        resource.close();
      }
    } catch (Exception e) {
      throw new ConfigException("Could not load resource: " + name, e);
    }
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory addPropertiesFromInputStream(InputStream inputStream) {
    Properties p = new Properties();
    p.putAll(this.properties);
    try {
      p.load(inputStream);
    } catch (Exception e) {
      throw new ConfigException("Could not load input stream", e);
    }
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory addProperty(String key, String value) {
    Properties p = new Properties();
    p.putAll(this.properties);
    p.setProperty(key, value);
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory removeProperty(String key) {
    Properties p = new Properties();
    p.putAll(this.properties);
    p.remove(key);
    return new ConfigurationFactory(p);
  }

  public ConfigurationFactory getConfigurationProvider(String path) {
    Properties p = new Properties();
    if (path == null || path.isEmpty()) {
      p.putAll(this.properties);
    } else {
      String prefix = path.endsWith(".")
        ? path
        : path + ".";
      for (String key : this.properties.stringPropertyNames()) {
        if (key.startsWith(prefix)) {
          String newKey = key.substring(prefix.length());
          p.setProperty(newKey, this.properties.getProperty(key));
        }
      }
    }
    return new ConfigurationFactory(p);
  }

  /**
   * Get full set of configuration represented as {@link Properties}.
   *
   * @return full configuration set
   * @throws IllegalStateException when provider is unable to fetch configuration
   */
  public Properties getProperties() {
    Properties p = new Properties();
    p.putAll(this.properties);
    return p;
  }

  /**
   * Get a configuration property value.
   *
   * @param key configuration key
   * @return configuration value
   * @throws NoSuchElementException when the provided {@code key} doesn't have a corresponding
   *                                config value
   */
  public String getProperty(String key) {
    String property = this.properties.getProperty(key);

    if (property == null) {
      throw new NoSuchElementException("No configuration with key: " + key);
    }

    return property;
  }

  /**
   * Get a configuration property of a given basic {@code type}. Sample call could look like:
   * <pre>
   *   boolean myBooleanProperty = configurationProvider.getProperty("my.property", boolean.class);
   * </pre>
   *
   * @param <T>  property type
   * @param key  configuration key
   * @param type {@link Class} for {@code <T>}
   * @return configuration value
   * @throws NoSuchElementException   when the provided {@code key} doesn't have a corresponding
   *                                  config value
   * @throws IllegalArgumentException when property can't be converted to {@code type}
   * @throws IllegalStateException    when provider is unable to fetch configuration value for the
   *                                  given {@code key}
   */
  public <T> T getProperty(String key, Class<T> type) {
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }

    String property = this.properties.getProperty(key);

    if (property == null) {
      throw new NoSuchElementException("No configuration with key: " + key);
    }

    Object value = typedValue(type, null, key, property);

    return type.cast(value);
  }

  /**
   * Create an instance of a given {@code type} that will be bound to this provider. Each time
   * configuration changes the bound object will be updated with the new values. Please note that
   * each method of returned object can throw runtime exceptions.
   *
   * @param type {@link Class} for {@code <T>}
   * @param <T>  interface describing configuration object to bind
   * @return configuration object bound to this {@link ConfigurationFactory}
   * @throws IllegalArgumentException when property can't be converted to {@code type}
   */
  public <T> T bind(Class<T> type) {
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null");
    }
    if (!type.isInterface()) {
      throw new IllegalArgumentException("type must be interface");
    }

    DynamicType.Builder<Object> subclassBuilder = new ByteBuddy()
      .subclass(Object.class);

    StringBuilder toStringBuilder = new StringBuilder(type.getSimpleName());
    toStringBuilder.append("{");

    final Method[] declaredMethods = type.getDeclaredMethods();

    for (Method method : declaredMethods) {
      final Class<?> returnType = method.getReturnType();
      if (returnType == Void.class) {
        continue;
      }
      final Type genericReturnType = method.getGenericReturnType();
      final String methodName = method.getName();
      final Config config = method.getAnnotation(Config.class);
      if (config != null) {
        String key = config.value();
        String stringValue = this.properties.getProperty(key, config.defaultValue());
        Object value = typedValue(returnType, genericReturnType, key, stringValue);

        subclassBuilder = subclassBuilder
          .method(named(methodName))
          .intercept(FixedValue.value(value));

        // for 'toString' method
        toStringBuilder.append('\n');
        toStringBuilder.append('\t');
        toStringBuilder.append(methodName);
        toStringBuilder.append("=");
        if (value.getClass().equals(String.class)) {
          toStringBuilder.append('\'');
          toStringBuilder.append(value.toString());
          toStringBuilder.append('\'');
        } else {
          toStringBuilder.append(value.toString());
        }
        toStringBuilder.append(",");
      }
    }

    toStringBuilder.delete(toStringBuilder.length() - 1, toStringBuilder.length());
    toStringBuilder.append('\n');
    toStringBuilder.append("}");

    // override 'toString' method
    subclassBuilder = subclassBuilder
      .method(named("toString"))
      .intercept(FixedValue.value(toStringBuilder.toString()));

    Class<?> dynamicType = subclassBuilder
      .implement(type)
      .make()
      .load(type.getClassLoader())
      .getLoaded();

    Object bound;
    try {
      bound = dynamicType.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return type.cast(bound);
  }

  private Object typedValue(Class<?> type, Type genericReturnType, String key, String value) {
    //
    // Common types
    //

    // String, String[]
    if (type.equals(String.class)) {
      return value;
    }
    if (type.equals(String[].class)) {
      return value.split(",");
    }
    // boolean, boolean[]
    if (type.equals(Boolean.class) || type.equals(boolean.class)) {
      return Boolean.valueOf(value);
    }
    if (type.equals(Boolean[].class) || type.equals(boolean[].class)) {
      String[] values = value.split(",");
      Boolean[] array = new Boolean[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Boolean.valueOf(values[i]);
      }
      return array;
    }
    // byte, byte[]
    if (type.equals(Byte.class) || type.equals(byte.class)) {
      return Byte.valueOf(value);
    }
    if (type.equals(Byte[].class) || type.equals(byte[].class)) {
      String[] values = value.split(",");
      Byte[] array = new Byte[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Byte.valueOf(values[i]);
      }
      return array;
    }
    // short, short[]
    if (type.equals(Short.class) || type.equals(short.class)) {
      return Short.valueOf(value);
    }
    if (type.equals(Short[].class) || type.equals(short[].class)) {
      String[] values = value.split(",");
      Short[] array = new Short[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Short.valueOf(values[i]);
      }
      return array;
    }
    // char, char[]
    if (type.equals(Character.class) || type.equals(char.class)) {
      if (value.length() != 1) {
        throw new IllegalArgumentException("Cannot parse argument type " + type);
      }
      return Character.valueOf(value.charAt(0));
    }
    if (type.equals(Character[].class) || type.equals(char[].class)) {
      String[] values = value.split(",");
      Character[] array = new Character[values.length];
      for (int i = 0; i < values.length; i++) {
        if (values[i].length() != 1) {
          throw new IllegalArgumentException("Cannot parse argument type " + type);
        }
        array[i] = Character.valueOf(values[i].charAt(0));
      }
      return array;
    }
    // int, int[]
    if (type.equals(Integer.class) || type.equals(int.class)) {
      return Integer.valueOf(value);
    }
    if (type.equals(Integer[].class) || type.equals(int[].class)) {
      String[] values = value.split(",");
      Integer[] array = new Integer[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Integer.valueOf(values[i]);
      }
      return array;
    }
    // long, long[]
    if (type.equals(Long.class) || type.equals(long.class)) {
      return Long.valueOf(value);
    }
    if (type.equals(Long[].class) || type.equals(long[].class)) {
      String[] values = value.split(",");
      Long[] array = new Long[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Long.valueOf(values[i]);
      }
      return array;
    }
    // float, float[]
    if (type.equals(Float.class) || type.equals(float.class)) {
      return Float.valueOf(value);
    }
    if (type.equals(Float[].class) || type.equals(float[].class)) {
      String[] values = value.split(",");
      Float[] array = new Float[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Float.valueOf(values[i]);
      }
      return array;
    }
    // double, double[]
    if (type.equals(Double.class) || type.equals(double.class)) {
      return Double.valueOf(value);
    }
    if (type.equals(Double[].class) || type.equals(double[].class)) {
      String[] values = value.split(",");
      Double[] array = new Double[values.length];
      for (int i = 0; i < values.length; i++) {
        array[i] = Double.valueOf(values[i]);
      }
      return array;
    }
    // Enum (extract from string)
    if (type.isEnum()) {
      return Enum.valueOf((Class<Enum>) type, value);
    }
    // Collection
    if (Collection.class.isAssignableFrom(type)) {
      String[] values = value.split(",");

      // should never happen
      if (values.length == 0) {
        return Collections.emptyList();
      }

      // special case, default value (empty String)
      if (values.length == 1 && values[0].trim().isEmpty()) {
        return Collections.emptyList();
      }

      if (genericReturnType != null) {
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        // Collection can have only one generic type
        Type actualType = parameterizedType.getActualTypeArguments()[0];

        if (!type.equals(String.class)) {
          Class<?> actualTypeClass = (Class<?>) actualType;

          Object[] array = (Object[]) Array.newInstance(actualTypeClass, values.length);

          for (int i = 0; i < values.length; i++) {
            Object val = typedValue(actualTypeClass, null, "", values[i]);
            array[i] = val;
          }

          return Arrays.asList(array);
        }
      }

      return Arrays.asList(values);
    }
    // Config
    if (type.isInterface() && !key.isEmpty() && containsConfigs(type)) {
      return this.getConfigurationProvider(key).bind(type);
    }

    throw new IllegalArgumentException("Unable to cast value \'" + value + "\' to " + type);
  }

  private static boolean containsConfigs(Class<?> type) {
    for (Method method : type.getDeclaredMethods()) {
      if (method.getAnnotation(Config.class) != null) {
        return true;
      }
    }
    return false;
  }

}
