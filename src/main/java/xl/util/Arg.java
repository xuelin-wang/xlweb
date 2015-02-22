/*
 * Copyright  20140616 Xuelin Wang, all rights reserved.
 */

package xl.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Arg {
  public int index() default -1;
  public String[] flags() default {};
  public String action() default "store";
  public String nargs() default "";
  public boolean required() default false;
  public String defaultVal() default "";
  public String constant() default "";
  public String validationType() default "";
  public String[] choices() default {};
  public String metavar() default "";
  public String propName() default "";
  public String help() default "";
  public String version() default "";
  public String validationRegex() default "";
}

