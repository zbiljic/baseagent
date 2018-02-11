package com.zbiljic.baseagent.instrument;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.utility.JavaModule;

import java.util.logging.Level;
import java.util.logging.Logger;

class ErrorLoggingListener extends AgentBuilder.Listener.Adapter {

  private static Logger logger = Logger.getLogger(ErrorLoggingListener.class.getName());

  @Override
  public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
    logger.log(Level.WARNING, "ERROR on transformation " + typeName, throwable);
  }
}
