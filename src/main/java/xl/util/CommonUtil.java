package xl.util;

import java.lang.reflect.Array;

public class CommonUtil {
  public static boolean objectEqual(Object a, Object b)
  {
    if (a == null)
      return b == null;
    else if (b == null)
      return false;
    Class<?> aClass = a.getClass();
    if (!(aClass.getName().equals(b.getClass().getName())))
      return false;
    if (aClass.isArray())
      return arrayEqual(a, b);
    return a.equals(b);
  }
  public static boolean arrayEqual(Object a, Object b)
  {
    if (a == null)
      return b == null;
    else if (b == null)
      return false;
    Class<?> aClass = a.getClass();
    if (!(aClass.getName().equals(b.getClass().getName())))
      return false;
    if (!aClass.isArray()) {
      throw new RuntimeException("a is not an array: " + aClass.getName());
    }
    
    if (! aClass.getComponentType().getName().equals(b.getClass().getComponentType().getName()))
        return false;
    
    int aLen = Array.getLength(a);
    int bLen = Array.getLength(b);
    if (aLen != bLen)
      return false;
    for (int index = 0; index < aLen; index++) {
      if (!objectEqual(Array.get(a, index), Array.get(b, index)))
        return false;
    }
    return true;
  }
}
