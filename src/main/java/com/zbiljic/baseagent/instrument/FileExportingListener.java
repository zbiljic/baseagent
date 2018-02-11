package com.zbiljic.baseagent.instrument;

import com.zbiljic.baseagent.util.IOUtils;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class FileExportingListener extends AgentBuilder.Listener.Adapter {

  private static Logger logger = Logger.getLogger(FileExportingListener.class.getName());

  private final Collection<String> exportClassesWithName;
  static final List<String> exportedClasses = new ArrayList<String>();

  FileExportingListener(Collection<String> exportClassesWithName) {
    this.exportClassesWithName = exportClassesWithName;
  }

  @Override
  public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
    if (!exportClassesWithName.contains(typeDescription.getName())) {
      return;
    }
    final File exportedClass;
    try {
      exportedClass = File.createTempFile(typeDescription.getName(), ".class");
      IOUtils.copy(new ByteArrayInputStream(dynamicType.getBytes()), new FileOutputStream(exportedClass));
      logger.log(Level.INFO, "Exported class modified by Byte Buddy: {0}", exportedClass.getAbsolutePath());
      exportedClasses.add(exportedClass.getAbsolutePath());
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }
}
