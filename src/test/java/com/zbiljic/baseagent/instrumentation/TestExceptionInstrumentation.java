package com.zbiljic.baseagent.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;

public class TestExceptionInstrumentation implements Instrumenter {

  @Override
  public AgentBuilder instrument(AgentBuilder agentBuilder) {
    throw new RuntimeException("This is an expected test exception. "
      + "It is thrown to test whether baseagent can cope with instrumenter that throw an exception.");
  }
}
