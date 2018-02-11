package com.zbiljic.baseagent.instrument;

import com.zbiljic.baseagent.instrumentation.Instrumenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * A helper class for loading all plugins from the classpath.
 */
public final class PluginBootstrap {

  private PluginBootstrap() { /* No instance methods */ }

  public static List<Instrumenter> loadInstrumenters() {
    List<Instrumenter> collection = loadFactories(Instrumenter.class);
    if (!collection.isEmpty()) {
      return Collections.unmodifiableList(Arrays.asList(collection.toArray(new Instrumenter[0])));
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * A helper method for loading factories from the classpath.
   */
  private static <T> List<T> loadFactories(Class<T> clazz) {
    ServiceLoader<T> factories = ServiceLoader.load(clazz);
    if (factories.iterator().hasNext()) {
      return extractFactories(factories.iterator());
    } else {
      // By default FactoryRegistry.load uses the TCCL, this may not be enough in environment
      // dealing with classloader's differently such as OSGi. So we should try to use the
      // classloader having loaded this class.
      factories = ServiceLoader.load(clazz, PluginBootstrap.class.getClassLoader());
      if (factories.iterator().hasNext()) {
        return extractFactories(factories.iterator());
      } else {
        return Collections.emptyList();
      }
    }
  }

  private static <T> List<T> extractFactories(Iterator<T> factoriesIterator) {
    final List<T> factoriesList = new ArrayList<T>();
    // use while(true) loop so that we can isolate all service loader errors from .next and .hasNext to a single service
    while (true) {
      try {
        if (!factoriesIterator.hasNext()) {
          break;
        }
        T factory = factoriesIterator.next();
        factoriesList.add(factory);
      } catch (ServiceConfigurationError error) {
        throw new IllegalArgumentException("Found error loading a factory", error);
      }
    }
    return factoriesList;
  }

}
