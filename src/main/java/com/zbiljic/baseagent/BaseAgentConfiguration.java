package com.zbiljic.baseagent;

import com.zbiljic.baseagent.configuration.Config;

import java.util.Collection;

public interface BaseAgentConfiguration {

  @Config(
    value = "baseagent.active",
    label = "Activate baseagent",
    description = "If set to `false` baseagent will be completely deactivated.",
    defaultValue = "true"
  )
  Boolean isBaseAgentActive();

  @Config(
    value = "baseagent.instrument.exclude",
    label = "Excluded packages",
    description = "Exclude packages and their sub-packages from the instrumentation (for example the profiler)."
  )
  Collection<String> getExcludePackages();

  @Config(
    value = "baseagent.instrument.excludeContaining",
    label = "Exclude containing",
    description = "Exclude classes from the instrumentation (for example from profiling) that "
      + "contain one of the following strings as part of their class name."
  )
  Collection<String> getExcludeContaining();

  @Config(
    value = "baseagent.instrument.include",
    label = "Included packages",
    description = "The packages that should be included for instrumentation. "
      + "All subpackages of the listed packages are included automatically. "
      + "You can exclude subpackages of a included package via `baseagent.instrument.exclude`. "
      + "Example: `org.somecompany.package, com.someothercompany`"
  )
  Collection<String> getIncludePackages();

  @Config(
    value = "baseagent.instrument.runtimeAttach",
    label = "Attach agent at runtime",
    description = "Attaches the agent via the Attach API at runtime and re-transforms all currently loaded classes.",
    defaultValue = "true"
  )
  Boolean isAttachAgentAtRuntime();

  @Config(
    value = "baseagent.instrument.exportGeneratedClassesWithName",
    label = "Export generated classes with name",
    description = "A list of the fully qualified class names which should be exported to the file "
      + "system after they have been modified by Byte Buddy. This option is useful to debug "
      + "problems inside the generated class. Classes are exported to a temporary directory. "
      + "The logs contain the information where the files are stored."
  )
  Collection<String> getExportClassesWithName();

  @Config(
    value = "baseagent.instrument.debug",
    label = "Debug instrumentation",
    description = "Set to true to log additional information and warnings during the instrumentation process."
  )
  Boolean isDebugInstrumentation();

  @Config(
    value = "baseagent.instrument.excludedInstrumenter",
    label = "Excluded Instrumenters",
    description = "A list of the simple class names of Instrumenter's that should not be applied."
  )
  Collection<String> getExcludedInstrumenters();

}
