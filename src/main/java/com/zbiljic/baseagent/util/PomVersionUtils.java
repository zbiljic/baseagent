package com.zbiljic.baseagent.util;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PomVersionUtils {

  private static Logger logger = Logger.getLogger(PomVersionUtils.class.getName());

  private PomVersionUtils() { /* No instance methods */ }

  public static String getVersionFromPomProperties(Class clazz, String groupId, String artifactId) {
    final String classpathLocation = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";

    final InputStream inputStream = clazz.getClassLoader().getResourceAsStream(classpathLocation);
    final Properties pomProperties = new Properties();
    try {
      if (inputStream == null) {
        return null;
      }
      pomProperties.load(inputStream);
      return pomProperties.getProperty("version");
    } catch (Exception e) {
      logger.log(Level.INFO, "Unable to load pom.properties for the '{0}:{1}', cause: {2}",
        new Object[]{groupId, artifactId, e.getMessage()});
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (Exception ignore) {
        }
      }
    }
    return null;
  }

  public static String getMavenCentralDownloadLink(String groupId, String artifactId, String version) {
    return String.format("http://central.maven.org/maven2/%s/%s/%s/%s-%s.jar", groupId.replace('.', '/'), artifactId, version, artifactId, version);
  }

}
