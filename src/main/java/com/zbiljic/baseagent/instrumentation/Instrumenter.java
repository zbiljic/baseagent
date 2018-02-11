package com.zbiljic.baseagent.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;

/**
 * Service provider interface for plug-ins that add bytecode instrumentation.
 */
public interface Instrumenter {

  /**
   * Adds bytecode instrumentation to the given {@link AgentBuilder}.
   *
   * @param agentBuilder the {@link AgentBuilder} object to which the additional instrumentation is
   *                     added
   * @return the {@link AgentBuilder} object with additional instrumentation added
   */
  AgentBuilder instrument(AgentBuilder agentBuilder);
}
