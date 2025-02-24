package org.jetbrains.jps.incremental.scala.sources;

import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.ex.JpsElementTypeWithDummyProperties;
import org.jetbrains.jps.model.module.JpsModuleType;

public class SharedSourcesModuleType extends JpsElementTypeWithDummyProperties implements JpsModuleType<JpsDummyElement> {
  public static final SharedSourcesModuleType INSTANCE = new SharedSourcesModuleType();
}
