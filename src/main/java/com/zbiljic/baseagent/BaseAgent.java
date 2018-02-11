package com.zbiljic.baseagent;

import com.zbiljic.baseagent.configuration.ConfigurationFactory;
import com.zbiljic.baseagent.instrument.AgentAttacher;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class BaseAgent {

  private static Logger logger = Logger.getLogger(BaseAgent.class.getName());

  private static BaseAgentConfiguration configuration;
  private static boolean initialized;

  private BaseAgent() { /* Intentionally private to enforce singleton pattern. */ }

  static {
    configuration = ConfigurationFactory.create()
      .addSystemProperties()
      .addPropertiesFromResource("/baseagent.properties")
      .bind(BaseAgentConfiguration.class);

    if (configuration.isDebugInstrumentation()) {
      logConfiguration();
    }
  }

  public static synchronized void init() {
    if (!initialized) {
      try {
        initialized = true;

        AgentAttacher.performRuntimeAttachment();

        logStatus();
      } catch (Throwable t) {
        logger.log(Level.SEVERE, t.getMessage(), t);
        throw new RuntimeException("Error attaching baseagent Java agent", t);
      }
    }
  }

  private static void logStatus() {
    logger.log(Level.INFO, "# baseagent status");
    logger.log(Level.INFO, "System information: {0}", getJvmAndOsVersionString());
  }

  private static String getJvmAndOsVersionString() {
    return "Java " + System.getProperty("java.version")
      + " (" + System.getProperty("java.vendor") + ") "
      + System.getProperty("os.name") + " " + System.getProperty("os.version");
  }

  private static void logConfiguration() {
    logger.log(Level.INFO, "# baseagent configuration");
    logger.log(Level.INFO, "{0}", configuration);
  }

  public static BaseAgentConfiguration getConfiguration() {
    return configuration;
  }
}
