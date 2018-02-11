package com.zbiljic.baseagent.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a methods as configurable and serves as a specification of to
 * express the structure, constraints and conditions under which instances can exist to fulfill
 * their intended purpose.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Config {

  /**
   * An informative name that clearly identifies the configuration in the system.
   * <p>
   * Good names are those which describe domain specific aspects already established in the system
   * architecture.
   *
   * @return configuration key name
   */
  String value();

  /**
   * A short description of the configuration.
   *
   * @return the configuration label
   */
  String label() default "";

  /**
   * An informative description that justify the existence of the configuration, putting it into
   * context for how it relates to high-level system concepts and outline what it is used for and
   * how changes affect the behaviour of the system.
   *
   * @return the configuration description
   */
  String description() default "";

  /**
   * Used to specify a default value for the field.
   *
   * @return the default value for the configuration
   */
  String defaultValue() default "";
}
