package com.zbiljic.baseagent.instrument;

import com.zbiljic.baseagent.BaseAgent;
import com.zbiljic.baseagent.BaseAgentConfiguration;
import com.zbiljic.baseagent.instrumentation.Instrumenter;
import com.zbiljic.baseagent.util.PomVersionUtils;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.zbiljic.baseagent.instrument.ConfigurationClassNameMatcher.configurationIgnored;
import static com.zbiljic.baseagent.instrumentation.ClassLoaderNameMatcher.classLoaderWithName;
import static com.zbiljic.baseagent.instrumentation.ClassLoaderNameMatcher.isReflectionClassLoader;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.nameContains;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * Attaches the {@link ByteBuddyAgent} at runtime and registers transformers.
 */
public class AgentAttacher {

  private static Logger logger = Logger.getLogger(AgentAttacher.class.getName());

  private static final BaseAgentConfiguration configuration = BaseAgent.getConfiguration();
  private static boolean runtimeAttached = false;
  private static Instrumentation instrumentation;

  private AgentAttacher() { /* No instance methods */ }

  /**
   * Attaches the profiler and other instruments at runtime so that it is not necessary to add the
   * -javaagent command line argument.
   */
  public static synchronized void performRuntimeAttachment() {
    if (runtimeAttached || !configuration.isBaseAgentActive() || !configuration.isAttachAgentAtRuntime()) {
      return;
    }
    runtimeAttached = true;

    if (initInstrumentation()) {
      final long start = System.currentTimeMillis();
      initByteBuddyClassFileTransformer();
      if (configuration.isDebugInstrumentation()) {
        logger.log(Level.INFO,
          "Attached agents in {0} ms", System.currentTimeMillis() - start);
      }
    }
  }

  private static boolean initInstrumentation() {
    try {
      instrumentation = getInstrumentation();
      return true;
    } catch (Exception e) {
      final String msg = "Failed to perform runtime attachment of the baseagent agent. Make sure "
        + "that you run your application with a JDK (not a JRE).\n"
        + "To make baseagent work with a JRE, you have to add the following command line argument "
        + "to the start of the JVM: -javaagent:/path/to/byte-buddy-agent-<version>.jar. "
        + "The version of the agent depends on the version of baseagent.\n"
        + "You can download the appropriate agent for the baseagent version you are using here: "
        + getByteBuddyAgentDownloadUrl();
      logger.log(Level.WARNING, msg, e);
      return false;
    }
  }

  private static Instrumentation getInstrumentation() {
    try {
      return ByteBuddyAgent.getInstrumentation();
    } catch (IllegalStateException e) {
      return ByteBuddyAgent.install();
    }
  }

  private static String getByteBuddyAgentDownloadUrl() {
    final String groupId = "net.bytebuddy";
    final String byteBuddyVersion = PomVersionUtils.getVersionFromPomProperties(ByteBuddy.class, groupId, "byte-buddy");
    return PomVersionUtils.getMavenCentralDownloadLink(groupId, "byte-buddy-agent", byteBuddyVersion);
  }

  private static void initByteBuddyClassFileTransformer() {
    AgentBuilder agentBuilder = createAgentBuilder();

    for (Instrumenter instrumenter : getInstrumenters()) {
      try {
        agentBuilder = instrumenter.instrument(agentBuilder);
      } catch (Throwable t) {
        if (configuration.isDebugInstrumentation()) {
          logger.log(Level.SEVERE, "Instrumentation [{0}] failure.", instrumenter.getClass().getSimpleName());
        }
      }
    }

    final long start = System.currentTimeMillis();
    try {
      agentBuilder.installOn(instrumentation);
    } finally {
      if (configuration.isDebugInstrumentation()) {
        logger.log(Level.INFO, "Installed agent in {0} ms", System.currentTimeMillis() - start);
      }
    }
  }

  private static AgentBuilder createAgentBuilder() {
    final ByteBuddy byteBuddy = new ByteBuddy()
      .with(TypeValidation.of(configuration.isDebugInstrumentation()))
      .with(MethodGraph.Compiler.DEFAULT);
    return new AgentBuilder.Default(byteBuddy)
      .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
      .with(getListener())
      .ignore(any(), isReflectionClassLoader())
      .or(any(), classLoaderWithName("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader"))
      // ignore some well-known
      .or(
        nameStartsWith("net.bytebuddy.")
          .or(nameStartsWith("org.aspectj."))
          .or(nameStartsWith("org.groovy."))
          .or(nameStartsWith("com.p6spy."))
          .or(nameStartsWith("org.slf4j.").and(not(nameStartsWith("org.slf4j.impl."))))
          .or(nameContains("javassist"))
          .or(nameContains(".asm."))
          .or(nameStartsWith("com.zbiljic.baseagent")
            .and(not(nameContains("Test")
                .or(nameContains("benchmark"))
              )
            )
          )
      )
      // ignore based on configuration
      .or(configurationIgnored())
      .disableClassFormatChanges();
  }

  private static AgentBuilder.Listener getListener() {
    List<AgentBuilder.Listener> listeners = new ArrayList<AgentBuilder.Listener>();
    if (configuration.isDebugInstrumentation()) {
      listeners.add(new ErrorLoggingListener());
    }
    if (!configuration.getExportClassesWithName().isEmpty()) {
      listeners.add(new FileExportingListener(configuration.getExportClassesWithName()));
    }
    return new AgentBuilder.Listener.Compound(listeners.toArray(new AgentBuilder.Listener[0]));
  }

  private static Iterable<Instrumenter> getInstrumenters() {
    List<Instrumenter> instrumenters = new ArrayList<Instrumenter>();
    for (Instrumenter instrumenter : PluginBootstrap.loadInstrumenters()) {
      try {
        if (!isExcluded(instrumenter)) {
          instrumenters.add(instrumenter);
          if (configuration.isDebugInstrumentation()) {
            logger.log(Level.INFO, "Registering {0}", instrumenter.getClass().getSimpleName());
          }
        } else if (configuration.isDebugInstrumentation()) {
          logger.log(Level.INFO, "Excluding {0}", instrumenter.getClass().getSimpleName());
        }
      } catch (NoClassDefFoundError e) {
        logger.log(Level.WARNING,
          String.format("NoClassDefFoundError when trying to apply %s. Make sure that optional types are not referenced directly.", instrumenter),
          e);
      }
    }
    return instrumenters;
  }

  private static boolean isExcluded(Instrumenter instrumenter) {
    return configuration.getExcludedInstrumenters().contains(instrumenter.getClass().getSimpleName());
  }

}
