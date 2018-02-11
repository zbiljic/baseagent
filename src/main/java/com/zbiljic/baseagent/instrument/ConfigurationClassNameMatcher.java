package com.zbiljic.baseagent.instrument;

import com.zbiljic.baseagent.BaseAgent;
import com.zbiljic.baseagent.BaseAgentConfiguration;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

//@formatter:off
/**
 * An {@link ElementMatcher} with the following logic:
 * <ul>
 *   <li>Exclude all types which contain <code>baseagent.instrument.excludeContaining</code></li>
 *   <li>Include all types <code>baseagent.instrument.include</code></li>
 *   <li>If there are no more specific excludes in <code>baseagent.instrument.exclude</code></li>
 * </ul>
 */
//@formatter:on
public class ConfigurationClassNameMatcher extends ElementMatcher.Junction.AbstractBase<TypeDescription> {

  private static Logger logger = Logger.getLogger(ConfigurationClassNameMatcher.class.getName());

  private static final ConfigurationClassNameMatcher INSTANCE = new ConfigurationClassNameMatcher();

  public static Junction<TypeDescription> configurationIgnored() {
    return INSTANCE;
  }

  private ConfigurationClassNameMatcher() { /* Intentionally private to enforce singleton pattern. */ }

  private static Collection<String> includes;

  private static Collection<String> excludes;

  private static Collection<String> excludeContaining;

  static {
    initIncludesAndExcludes();
  }

  private static void initIncludesAndExcludes() {
    BaseAgentConfiguration configuration = BaseAgent.getConfiguration();

    excludeContaining = new ArrayList<String>(configuration.getExcludeContaining().size());
    excludeContaining.addAll(configuration.getExcludeContaining());

    excludes = new ArrayList<String>(configuration.getExcludePackages().size());
    excludes.add("com.zbiljic.baseagent");
    excludes.addAll(configuration.getExcludePackages());

    includes = new ArrayList<String>(configuration.getIncludePackages().size());
    includes.addAll(configuration.getIncludePackages());
    if (includes.isEmpty()) {
      logger.log(Level.WARNING,
        "No includes for instrumentation configured. Please set the baseagent.instrument.include property.");
    }
  }

  //@formatter:off
  /**
   * Checks if a specific class should be ignored.
   * <p>
   * The default implementation considers the following properties:
   * <ul>
   *   <li><code>baseagent.instrument.excludeContaining</code></li>
   *   <li><code>baseagent.instrument.include</code></li>
   *   <li><code>baseagent.instrument.exclude</code></li>
   * </ul>
   *
   * @param className The name of the class. For example java/lang/String
   * @return <code>true</code>, if the class should be ignored, <code>false</code> otherwise
   */
  //@formatter:on
  public static boolean shouldIgnore(String className) {
    for (String exclude : excludeContaining) {
      if (className.contains(exclude)) {
        return true;
      }
    }
    for (String include : includes) {
      if (className.startsWith(include)) {
        return hasMoreSpecificExclude(className, include);
      }
    }
    return false;
  }

  private static boolean hasMoreSpecificExclude(String className, String include) {
    for (String exclude : excludes) {
      if (exclude.length() > include.length() && exclude.startsWith(include) && className.startsWith(exclude)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean matches(TypeDescription target) {
    return shouldIgnore(target.getName());
  }
}
