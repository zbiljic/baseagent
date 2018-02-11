package com.zbiljic.baseagent.instrument;

import com.zbiljic.baseagent.BaseAgent;
import com.zbiljic.baseagent.instrumentation.Instrumenter;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.utility.JavaModule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileExportingListenerTest {

  @BeforeClass
  public static void setUp() throws Exception {
    BaseAgent.init();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    for (String exportedClass : FileExportingListener.exportedClasses) {
      new File(exportedClass).delete();
    }
  }

  @Test
  public void testExportIncludedClasses() throws Exception {
    new ExportMe();
    assertEquals(1, FileExportingListener.exportedClasses.size());
    assertTrue(FileExportingListener.exportedClasses.get(0).contains(ExportMe.class.getName()));
    final File exportedClass = new File(FileExportingListener.exportedClasses.get(0));
    assertTrue(exportedClass.exists());
  }

  private static class ExportMe {

    public void test() {
    }
  }

  public static class ExportMeInstrumentation implements Instrumenter {

    @Override
    public AgentBuilder instrument(AgentBuilder agentBuilder) {
      return agentBuilder
        .type(is(ExportMe.class))
        .transform(new Transformer() {
          @Override
          public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                  TypeDescription typeDescription,
                                                  ClassLoader classLoader,
                                                  JavaModule module) {
            return builder.constructor(any())
              .intercept(SuperMethodCall.INSTANCE);
          }
        });
    }
  }

}
