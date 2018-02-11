package com.zbiljic.baseagent.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;

public class TestInstrumentation implements Instrumenter {

  @Override
  public AgentBuilder instrument(AgentBuilder agentBuilder) {
    return agentBuilder;
  }
}
