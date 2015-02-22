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
public @interface Args {
  public Arg[] value() default{};
}

