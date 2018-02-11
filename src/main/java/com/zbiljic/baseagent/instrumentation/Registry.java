package com.zbiljic.baseagent.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.RawMatcher;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.supportsModules;

public final class Registry {

  private Registry() { /* No instance methods */ }

  public interface Registrable {

    AgentBuilder register(AgentBuilder agentBuilder);
  }

  public static Builder type(RawMatcher matcher) {
    return new Builder(matcher);
  }

  public static Builder type(ElementMatcher<? super TypeDescription> typeMatcher) {
    return type(typeMatcher, any());
  }

  public static Builder type(ElementMatcher<? super TypeDescription> typeMatcher,
                             ElementMatcher<? super ClassLoader> classLoaderMatcher) {
    return type(typeMatcher, classLoaderMatcher, any());
  }

  public static Builder type(ElementMatcher<? super TypeDescription> typeMatcher,
                             ElementMatcher<? super ClassLoader> classLoaderMatcher,
                             ElementMatcher<? super JavaModule> moduleMatcher) {
    return type(new RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, not(supportsModules()).or(moduleMatcher)));
  }

  public static class Builder {

    protected final RawMatcher rawMatcher;

    private Builder(RawMatcher rawMatcher) {
      this.rawMatcher = rawMatcher;
    }

    public Registrable visit(AsmVisitorWrapper asmVisitorWrapper) {
      requireNonNull(asmVisitorWrapper, "asmVisitorWrapper cannot be null");
      return new VisitBuilder(rawMatcher, asmVisitorWrapper);
    }

    protected AgentBuilder.Identified.Narrowable narrow(AgentBuilder agentBuilder) {
      return agentBuilder.type(rawMatcher);
    }
  }

  public static class VisitBuilder extends Builder implements Registrable {

    private final AsmVisitorWrapper asmVisitorWrapper;

    private VisitBuilder(RawMatcher rawMatcher, AsmVisitorWrapper asmVisitorWrapper) {
      super(rawMatcher);
      this.asmVisitorWrapper = asmVisitorWrapper;
    }

    @Override
    public AgentBuilder register(AgentBuilder agentBuilder) {
      return narrow(agentBuilder)
        .transform(new Transformer() {
          @Override
          public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                  TypeDescription typeDescription,
                                                  ClassLoader classLoader,
                                                  JavaModule module) {
            return builder.visit(asmVisitorWrapper);
          }
        });
    }
  }

  private static <T> T requireNonNull(T obj, String message) {
    if (obj == null) {
      throw new NullPointerException(message);
    }
    return obj;
  }
}
