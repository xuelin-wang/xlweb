/*
 * Copyright  20140616 Xuelin Wang, all rights reserved.
 */

package xl.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

public class CommandWrapper {
  private static final String NARGS_PLUS = "+";
  private static final String NARGS_STAR = "*";
  private static final String NARGS_QUESTION = "?";
  private List<String> _followingArgs;
  private List<String> _positionals;
  private int _argIndex;
  private static String[] _args;
  private Arg[] _cmdArgs;
  private Map<String, Arg> _flagToOptionalArg;
  private List<Arg> _positionalArgs;
  private Map<Integer, ArgValue> _argsMap;
  private Class<?> _clazz;
  private Method _theMethod;
  private Object[] _actualArgs;
  private boolean _isVersion;
  private String _version;
  private boolean _isHelp;

  static enum Action {
    STORE("store"), STORE_CONST("store_const"), STORE_TRUE("store_true"), STORE_FALSE(
        "store_false"), APPEND("append"), APPEND_CONST("append_const"), COUNT(
        "count"), HELP("help"), VERSION("version");
    private String _name;

    private Action(String name) {
      _name = name;
    }

    public String getName() {
      return _name;
    }

    public static Action fromName(String name) {
      for (Action action : Action.values()) {
        if (action.getName().equals(name))
          return action;
      }
      throw new RuntimeException("Unsupported action: " + name);
    }
  }

  private static class ArgValue {
    private Map<String, ArgValue> _propToValue;
    private List<String> _values;

    public ArgValue() {
      this(null);
    }

    public ArgValue(List<String> vals) {
      _propToValue = new HashMap<String, ArgValue>();
      _values = new ArrayList<String>();
      if (vals != null)
        _values.addAll(vals);
    }

    public List<String> getValues() {
      return Collections.unmodifiableList(_values);
    }

    public Map<String, ArgValue> getPropToValue() {
      return Collections.unmodifiableMap(_propToValue);
    }

    public List<String> getPropValue(String propName) {
      if (propName.length() == 0) {
        return _values;
      }

      int index = propName.indexOf('.');
      if (index < 0) {
        ArgValue propArgValue = _propToValue.get(propName);
        if (propArgValue == null)
          return null;
        else
          return propArgValue._values;
      }

      String name1 = propName.substring(0, index);
      ArgValue propArgValue = _propToValue.get(name1);
      if (propArgValue == null)
        return null;

      String restName = propName.substring(index + 1);
      return propArgValue.getPropValue(restName);
    }

    private void addArgValue(String propName, List<String> vals,
        ArgValue argVal, boolean errorIfExists, boolean removeExisting) {
      if (vals == null || vals.size() == 0)
        return;
      if (propName.length() == 0) {
        if (errorIfExists && argVal._values.size() > 0) {
          throw new RuntimeException("Value already exists for prop: "
              + propName);
        }
        if (removeExisting)
          argVal._values.clear();
        argVal._values.addAll(vals);
      } else {
        int index = propName.indexOf('.');
        if (index < 0) {
          ArgValue tmpArgVal = new ArgValue(vals);
          argVal._propToValue.put(propName, tmpArgVal);
        } else {
          String name1 = propName.substring(0, index);
          ArgValue tmpArgVal = argVal._propToValue.get(name1);
          if (tmpArgVal == null) {
            tmpArgVal = new ArgValue();
            argVal._propToValue.put(name1, tmpArgVal);
          }
          String restName = propName.substring(index + 1);
          addArgValue(restName, vals, tmpArgVal, errorIfExists, removeExisting);
        }
      }
    }

    public void addPropValues(String propName, List<String> vals) {
      addArgValue(propName, vals, this, false, false);
    }

    public void addPropValue(String propName, String val) {
      addArgValue(propName, Collections.singletonList(val), this, false, false);
    }

    public void setPropValues(String propName, List<String> vals) {
      addArgValue(propName, vals, this, true, false);
    }

    public void setPropValue(String propName, String val) {
      addArgValue(propName, Collections.singletonList(val), this, true, false);
    }

    public void replacePropValues(String propName, List<String> vals) {
      addArgValue(propName, vals, this, false, true);
    }

    public void replacePropValue(String propName, String val) {
      addArgValue(propName, Collections.singletonList(val), this, false, true);
    }
  }

  private void usage(String msg, boolean throwEx) {
    System.err
        .println(getUsageString());
    if (throwEx)
      throw new RuntimeException(msg);
    else
      if (msg != null)
        System.err.println(msg);
  }

  public static void main(String[] args) throws NoSuchMethodException,
      InstantiationException, IllegalAccessException,
      InvocationTargetException, ClassNotFoundException {
    PrintStream original = System.out;
    PrintStream nullPs = new PrintStream(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        // DO NOTHING
      }
    });
    System.setOut(nullPs);

    Object retval = CommandWrapper.execute(args);
    System.setOut(original);
    System.out.println(String.valueOf(retval));
  }

  public CommandWrapper(String[] args) {
    _args = args;
  }

  private static Method findMethod(Class<?> clazz, String methodName) {
    Method theMethod = null;
    Class<?> currClazz = clazz;
    while (currClazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (!methodName.equals(method.getName()))
          continue;
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
          continue;
        theMethod = method;
        break;
      }
      if (theMethod != null)
        break;
      currClazz = currClazz.getSuperclass();
    }

    return theMethod;
  }

    private Arg[] getArgs(Method method)
    {
        Args args = method.getAnnotation(Args.class);
        return args.value();
    }
  private void findArgs(Class<?> clazz, String methodName) {
    Class<?> currClazz = clazz;
    while (currClazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (!methodName.equals(method.getName()))
          continue;
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
          continue;
        _cmdArgs = getArgs(method);
        if (_cmdArgs.length > 0)
          return;
      }
      currClazz = currClazz.getSuperclass();
    }

    for (Class<?> face : clazz.getInterfaces()) {
      for (Method method : face.getDeclaredMethods()) {
        if (!methodName.equals(method.getName()))
          continue;
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers))
          continue;
          _cmdArgs = getArgs(method);
        if (_cmdArgs.length > 0)
          return;
      }
    }

    Class<?>[] paramTypes = _theMethod.getParameterTypes();
    int paramCount = paramTypes.length;
    _cmdArgs = new Arg[paramCount];
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      final int finalParamIndex = paramIndex;
      _cmdArgs[paramIndex] = new Arg() {

        @Override
        public Class<? extends Annotation> annotationType() {
          return Arg.class;
        }

        @Override
        public int index() {
          return finalParamIndex;
        }

        @Override
        public String[] flags() {
          return new String[] { "-" + (char) ('a' + finalParamIndex) };
        }

        @Override
        public String action() {
          return Action.APPEND.getName();
        }

        @Override
        public String nargs() {
          return "";
        }

        @Override
        public boolean required() {
          return false;
        }

        @Override
        public String defaultVal() {
          return null;
        }

        @Override
        public String constant() {
          return null;
        }

        @Override
        public String validationType() {
          return null;
        }

        @Override
        public String[] choices() {
          return null;
        }

        @Override
        public String metavar() {
          return null;
        }

        @Override
        public String propName() {
          return "";
        }

        @Override
        public String help() {
          return null;
        }

        @Override
        public String validationRegex() {
          return null;
        }

        @Override
        public String version() {
          // TODO Auto-generated method stub
          return null;
        }
      };
    }
  }

  public void inspectCmdArgs() {
    if (_cmdArgs == null)
      return;

    Set<String> currFlags = new HashSet<String>();
    for (Arg cmdArg : _cmdArgs) {
      String[] flags = cmdArg.flags();
      if (cmdArg.action().equals(Action.VERSION.getName()) || cmdArg.action().equals(Action.HELP.getName())) {
        if (flags.length == 0)
          throw new RuntimeException("Version and help argument must specify a flag.");
      }
      else if (cmdArg.index() < 0) {
        throw new RuntimeException("Param index must not be negative for parameter " + cmdArg.metavar());
      }
      
      if (flags.length == 0) {
        _positionalArgs.add(cmdArg);
      } else {
        for (String flag : flags) {
          if (currFlags.contains(flag)) {
            throw new RuntimeException("Duplicate flag specified: " + flag);
          }
          currFlags.add(flag);
          _flagToOptionalArg.put(flag, cmdArg);
        }
      }
    }
  }

  public static Object execute(String[] args) throws NoSuchMethodException,
      InstantiationException, IllegalAccessException,
      InvocationTargetException, ClassNotFoundException {
    CommandWrapper commandCaller = new CommandWrapper(args);
    return commandCaller.execute();
  }

  public Object execute() throws ClassNotFoundException, NoSuchMethodException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    if (_args.length < 2)
      usage(null, true);

    _clazz = Class.forName(_args[0]);
    String methodName = _args[1];
    _theMethod = findMethod(_clazz, methodName);

    findArgs(_clazz, methodName);

    _flagToOptionalArg = new HashMap<String, Arg>();
    _positionalArgs = new ArrayList<Arg>();

    inspectCmdArgs();

    gatherArgs();

    createArgs();

    return invoke();
  }

  private String getUsageString() {
    StringBuilder sb = new StringBuilder();

    sb.append("usage: bin/run xuelin.CommandWrapper ")
        .append(_clazz.getName()).append(" ").append(_theMethod.getName())
        .append(" \\\n  ");

    StringBuilder optArgsFormat = new StringBuilder();
    StringBuilder posArgsFormat = new StringBuilder();
    StringBuilder optArgsMsg = new StringBuilder();
    StringBuilder posArgsMsg = new StringBuilder();

    for (Arg cmdArg : _cmdArgs) {
      String[] flags = cmdArg.flags();
      boolean positional = flags == null || flags.length == 0;
      StringBuilder formatMessage = new StringBuilder();
      boolean required = cmdArg.required();
      if (!required)
        formatMessage.append("[");
      if (!positional) {
        formatMessage.append(Joiner.on(", ").join(flags)).append(" ");
      }
      String nargs = cmdArg.nargs();
      String metavar = cmdArg.metavar();
      if (nargs.equals(NARGS_QUESTION))
        formatMessage.append("[").append(metavar).append("]");
      else if (nargs.equals(NARGS_PLUS))
        formatMessage.append(metavar).append(" [").append(metavar)
            .append(" ...]");
      else if (nargs.equals(NARGS_STAR))
        formatMessage.append(" [").append(metavar).append(" ...]");
      else
        formatMessage.append(metavar);
      if (!required)
        formatMessage.append("]");

      StringBuilder formatMsg = positional ? posArgsFormat : optArgsFormat;
      if (formatMsg.length() > 0)
        formatMsg.append(" ");
      formatMsg.append(formatMessage);
      
      StringBuilder argsMsg = positional ? posArgsMsg : optArgsMsg;
      argsMsg.append("  ").append(formatMsg).append("    ");
      String help = cmdArg.help();
      argsMsg.append(help);
      if (help.length() > 0) {
        if (!help.endsWith("."))
          argsMsg.append(".");
        argsMsg.append(" ");
      }
      
      String defaultVal = cmdArg.defaultVal();
      if (defaultVal.length() > 0) {
        argsMsg.append("Default is " + defaultVal + ". ");
      }
      
      String constant = cmdArg.constant();
      if (constant.length() > 0) {
        argsMsg.append("If specified flag without value, use " + constant + ". ");
      }
      
      String validationType = cmdArg.validationType();
      String validationRegex = cmdArg.validationRegex();
      String[] choices = cmdArg.choices();
      boolean hasValType = validationType.length() > 0 && !validationType.equals("string");
      boolean hasValRegex = validationRegex != null && validationRegex.length() > 0;
      boolean hasChoices = choices.length > 0;
      if (hasValType || hasValRegex || hasChoices) {
        argsMsg.append("Value must ");
        boolean started = false;
        if (hasValType) {
          argsMsg.append(" be ").append(validationType);
          started = true;
        }
        if (hasValRegex) {
          if (started)
            argsMsg.append(", ");
          argsMsg.append(" matches \"").append(validationRegex).append("\", ");
          started = true;
        }
        if (hasChoices){
          argsMsg.append(", ");
          argsMsg.append(" be one of: ").append(Joiner.on(", ").join(choices)).append(", ");
        }
        argsMsg.append(". ");
      }
      argsMsg.append("\n");
    }

    sb.append(optArgsFormat);
    if (posArgsFormat.length() > 0) {
      if (optArgsFormat.length() > 0)
        sb.append(" ");
      sb.append(posArgsFormat);
    }
    
    if (posArgsMsg.length() > 0) {
      sb.append("\n\npositional arguments:\n");
      sb.append(posArgsMsg);
    }
    if (optArgsMsg.length() > 0) {
      sb.append("\n\noptional arguments:\n");
      sb.append(posArgsMsg);
    }
    return sb.toString();
  }

  private void createArgs() {
    Class<?>[] paramTypes = _theMethod.getParameterTypes();
    Type[] genericTypes = _theMethod.getGenericParameterTypes();
    int paramCount = paramTypes.length;
    _actualArgs = new Object[paramCount];

    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      ArgValue argValue = _argsMap.get(paramIndex);
      if (argValue == null) {
        argValue = new ArgValue(Collections.<String>emptyList());
      }
      _actualArgs[paramIndex] = createObject(paramIndex,
          paramTypes[paramIndex], genericTypes[paramIndex], argValue);
    }

  }

  private Object createObject(int index, Class<?> paramType, Type type,
      ArgValue argValue) {
    if (isSimpleConvert(paramType)) {
      if (argValue.getPropToValue().size() > 0)
        throw new RuntimeException(
            "Simple type must not have prop value. Index: " + index + " type: "
                + paramType.getName());

      if (argValue.getValues().size() > 1) {
        throw new RuntimeException(
            "primitive arg must not be multiple, param index: " + index
                + ", type: " + paramType.getName());
      }

      List<String> strs = argValue.getValues();
      if (strs.size() == 0)
        return simpleConvert(null, paramType, false);
      return simpleConvert(strs.get(0), paramType, false);
    }

    try {
      return complexConvert(index, paramType, type, argValue);
    } catch (Exception ex) {
      usage(ex.getMessage(), true);
      return null;
    }
  }

  private Object complexConvert(int index, Class<?> paramType,
      Type genericType, ArgValue argValue) throws InstantiationException,
      IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, NoSuchMethodException, SecurityException {

    if (argValue.getPropToValue().size() == 0
        && argValue.getValues().size() == 0)
      return null;

    if (Collection.class.isAssignableFrom(paramType))
      return createCollection(index, paramType,
          (ParameterizedType) genericType, argValue);
    else if (Map.class.isAssignableFrom(paramType))
      return createMap(index, paramType, (ParameterizedType) genericType,
          argValue);
    else if (paramType.isArray())
      return createArray(index, paramType, genericType, argValue);
    else
      return createSimpleOrPrimObject(paramType, argValue);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object createCollection(int index, Class<?> paramType,
      ParameterizedType genericParamType, ArgValue argValue)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException,
      NoSuchMethodException, SecurityException {
    // only support collection of simple types
    Collection coll;
    if (!paramType.isInterface()
        && !Modifier.isAbstract(paramType.getModifiers())) {
      coll = (Collection) paramType.getConstructor().newInstance();
    } else if (paramType.isAssignableFrom(ArrayList.class)) {
      coll = new ArrayList();
    } else if (paramType.isAssignableFrom(HashSet.class)) {
      coll = new HashSet();
    } else if (paramType.isAssignableFrom(PriorityQueue.class)) {
      coll = new PriorityQueue<>();
    } else
      throw new RuntimeException("Unsupported collection class: "
          + paramType.getName());

    if (argValue.getPropToValue().size() > 0) {
      throw new RuntimeException("Collection props set not supported. Props: "
          + Joiner.on(", ").join(argValue.getPropToValue().keySet()));
    }
    List<String> vals = argValue.getValues();
    if (vals.size() > 0) {
      Type[] types = genericParamType.getActualTypeArguments();
      if (types.length != 1)
        throw new RuntimeException(
            "Expecting one generic type argument for colleciton def");
      Class<?> clazz = (Class<?>) types[0];
      for (String val : vals) {
        coll.add(simpleConvert(val, clazz, false));
      }
    }
    return coll;
  }

  private Object createArray(int index, Class<?> paramType, Type genericType,
      ArgValue argValue) {
    Class<?> clazz = paramType.getComponentType();
    List<String> vals = argValue.getValues();
    Object arr = Array.newInstance(clazz, vals.size());
    if (vals.size() > 0) {
      int i = 0;
      for (String val : vals) {
        Array.set(arr, i++, simpleConvert(val, clazz, false));
      }
    }
    return arr;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object createMap(int index, Class<?> paramType,
      ParameterizedType genericType, ArgValue argValue)
      throws InstantiationException, IllegalAccessException {
    Map map;
    if (!paramType.isInterface()
        && ((paramType.getModifiers() & Modifier.ABSTRACT) == 0))
      map = (Map) paramType.newInstance();
    else if (paramType.isAssignableFrom(HashMap.class))
      map = new HashMap();
    else
      throw new RuntimeException("Unsupported map type: " + paramType.getName());
    ArgValue keyArgVals = argValue.getPropToValue().get("key");
    ArgValue valArgVals = argValue.getPropToValue().get("value");
    if (keyArgVals != null) {
      Type[] types = genericType.getActualTypeArguments();
      Class<?> keyClazz = (Class<?>) types[0];
      Class<?> valClazz = (Class<?>) types[1];
      List<String> keys = keyArgVals.getValues();
      List<String> vals = valArgVals.getValues();
      for (int i = 0; i < keys.size(); i++) {
        String key = keys.get(i);
        Object keyObj = simpleConvert(key, keyClazz, false);
        String val = vals.get(i);
        Object valObj = simpleConvert(val, valClazz, false);
        map.put(keyObj, valObj);
      }
    }
    return map;
  }

  private Object createSimpleOrPrimObject(Class<?> paramType, ArgValue argValue)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    if (isSimpleConvert(paramType)) {
      if (argValue.getPropToValue().size() > 0)
        throw new RuntimeException(
            "Simple type must not have prop value. type: "
                + paramType.getName());

      if (argValue.getValues().size() > 1) {
        throw new RuntimeException("Primitive arg must not be multiple, type: "
            + paramType.getName());
      }

      List<String> strs = argValue.getValues();

      if (strs.size() == 0)
        return simpleConvert(null, paramType, false);
      return simpleConvert(strs.get(0), paramType, false);
    }

    return createSimpleObject(paramType, argValue);
  }

  private Object createSimpleObject(Class<?> paramType, ArgValue argValue)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    List<String> vals = argValue.getValues();
    Object obj = construct(paramType, vals);

    for (Map.Entry<String, ArgValue> entry : argValue.getPropToValue()
        .entrySet()) {
      String name = entry.getKey();
      ArgValue argVal = entry.getValue();
      setObjectProp(paramType, obj, name, argVal);
    }
    return obj;
  }

  private String propToSetMethodName(String prop) {
    return "set" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
  }

  private void setObjectProp(Class<?> paramType, Object obj, String name,
      ArgValue argVal) throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    String setMethodName = propToSetMethodName(name);
    Method foundMethod = null;
    for (Method method : paramType.getMethods()) {
      if (method.getName().equals(setMethodName)
          && (method.getModifiers() & Modifier.PUBLIC) != 0) {
        foundMethod = method;
        break;
      }
    }
    if (foundMethod == null)
      throw new RuntimeException("Cannot find public method: " + setMethodName
          + " of type: " + paramType.getName());

    Class<?>[] paramTypes = foundMethod.getParameterTypes();
    if (paramTypes.length != 1)
      throw new RuntimeException("Cannot find public method with one param: "
          + setMethodName + " of type: " + paramType.getName()
          + ", param count: " + paramTypes.length);

    Class<?> tmpType = paramTypes[0];
    Object arg = createSimpleOrPrimObject(tmpType, argVal);
    foundMethod.invoke(obj, arg);
  }

  private Object construct(Class<?> paramType, List<String> vals)
      throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    Constructor<?>[] constructors = paramType.getConstructors();
    int valsLen = vals.size();
    for (Constructor<?> constructor : constructors) {
      if ((constructor.getModifiers() & Modifier.PUBLIC) == 0
          || (constructor.getModifiers() & Modifier.ABSTRACT) != 0)
        continue;
      Class<?>[] paramTypes = constructor.getParameterTypes();
      if (paramTypes.length != valsLen)
        continue;
      Object[] args = new Object[valsLen];
      boolean good = true;
      for (int index = 0; index < paramTypes.length; index++) {
        // only support simple types
        Class<?> tmpType = paramTypes[index];
        String val = vals.get(index);
        try {
          args[index] = simpleConvert(val, tmpType, false);
        } catch (Exception ex) {
          good = false;
          break;
        }
      }
      if (!good)
        continue;
      return constructor.newInstance(args);
    }
    throw new RuntimeException("Cannot find suitable constructor for vals: "
        + Joiner.on(", ").join(vals));
  }

  private Object getCompatibleOrConvertValue(boolean returnCompatibleOrValue,
      boolean compatible, Object val, String msg) {
    if (returnCompatibleOrValue)
      return compatible;

    if (compatible)
      return val;
    else {
      usage(msg, true);
      return null;
    }
  }

  private static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  private boolean isSimpleConvert(Class<?> type) {
    String typeName = type.getName();
    return type.isPrimitive() || typeName.equals(String.class.getName())
        || typeName.equals(Character.class.getName())
        || typeName.equals(Boolean.class.getName())
        || typeName.equals(Byte.class.getName())
        || typeName.equals(Short.class.getName())
        || typeName.equals(Integer.class.getName())
        || typeName.equals(Long.class.getName())
        || typeName.equals(Float.class.getName())
        || typeName.equals(Double.class.getName());
  }

  private Object getDefaultPrimitiveValue(String typeName) {
    if (Boolean.TYPE.getName().equals(typeName))
      return false;
    else if (Character.TYPE.getName().equals(typeName))
      return ' ';
    else if (Byte.TYPE.getName().equals(typeName))
      return (byte) 0;
    else if (Short.TYPE.getName().equals(typeName))
      return (short) 0;
    else if (Integer.TYPE.getName().equals(typeName))
      return (int) 0;
    else if (Long.TYPE.getName().equals(typeName))
      return (long) 0;
    else if (Float.TYPE.getName().equals(typeName))
      return (float) 0;
    else if (Double.TYPE.getName().equals(typeName))
      return (double) 0;
    else {
      usage("Unsupported primitive type: " + typeName, true);
      return null;
    }
  }

  private Object simpleConvert(String str, Class<?> type,
      boolean returnCompatibleOrValue) {
    String typeName = type.getName();
    if (isEmpty(str)) {
      if (type.isPrimitive()) {
        return getDefaultPrimitiveValue(typeName);
      }
    }

    if (typeName.equals(String.class.getName()))
      return getCompatibleOrConvertValue(returnCompatibleOrValue, true, str,
          null);

    if (Boolean.TYPE.getName().equals(typeName)
        || Boolean.class.getName().equals(typeName)) {
      if (isEmpty(str)) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      }
      if ("true".compareToIgnoreCase(str) == 0)
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true,
            Boolean.TRUE, null);
      else if ("false".compareToIgnoreCase(str) == 0)
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true,
            Boolean.FALSE, null);
      else
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null, "Invalid boolean value: " + str);
    }

    if (Character.TYPE.getName().equals(typeName)
        || Character.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      if (str.length() == 1) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true,
            new Character(str.charAt(0)), null);
      } else
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null, "Invalid chracter value: " + str);
    }

    if (Byte.TYPE.getName().equals(typeName)
        || Byte.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      try {
        Byte b = Byte.valueOf(str);
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, b,
            null);
      } catch (Exception ex) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null, "Invalid byte value: " + str + ". Error: " + ex.getMessage());
      }
    }

    if (Short.TYPE.getName().equals(typeName)
        || Short.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      try {
        Short b = Short.valueOf(str);
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, b,
            null);
      } catch (Exception ex) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null, "Invalid short value: " + str + ". Error: " + ex.getMessage());
      }
    }

    if (Integer.TYPE.getName().equals(typeName)
        || Integer.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      try {
        Integer b = Integer.valueOf(str);
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, b,
            null);
      } catch (Exception ex) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null,
            "Invalid integer value: " + str + ". Error: " + ex.getMessage());
      }
    }

    if (Long.TYPE.getName().equals(typeName)
        || Long.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      try {
        Long b = Long.valueOf(str);
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, b,
            null);
      } catch (Exception ex) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null, "Invalid long value: " + str + ". Error: " + ex.getMessage());
      }
    }

    if (Float.TYPE.getName().equals(typeName)
        || Float.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      try {
        Float b = Float.valueOf(str);
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, b,
            null);
      } catch (Exception ex) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null, "Invalid float value: " + str + ". Error: " + ex.getMessage());
      }
    }

    if (Double.TYPE.getName().equals(typeName)
        || Double.class.getName().equals(typeName)) {
      if (isEmpty(str))
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, null,
            null);
      try {
        Double b = Double.valueOf(str);
        return getCompatibleOrConvertValue(returnCompatibleOrValue, true, b,
            null);
      } catch (Exception ex) {
        return getCompatibleOrConvertValue(returnCompatibleOrValue, false,
            null,
            "Invalid double value: " + str + ". Error: " + ex.getMessage());
      }
    }

    throw new RuntimeException("Should never be here");
  }

  private Arg checkFlag(String arg) {
    if (arg.startsWith("-")) {
      Arg cmdArg = _flagToOptionalArg.get(arg);
      if (cmdArg != null) {
        return cmdArg;
      }
      try {
        Double.parseDouble(arg);
        return null;
      } catch (Exception ex) {
        usage("Unrecognized flag: " + arg, true);
      }
    }
    return null;
  }

  private int sanitycheckNargs(String nargsStr) {
    int nargsCount = -1;
    if (nargsStr.length() > 0 && !nargsStr.equals(NARGS_QUESTION)
        && !nargsStr.equals(NARGS_STAR) && !nargsStr.equals(NARGS_PLUS)) {
      try {
        nargsCount = Integer.parseInt(nargsStr);
        if (nargsCount <= 0)
          throw new RuntimeException(
              "nargs must be +, ?, * or a position number: " + nargsStr);
      } catch (Exception ex) {
        throw new RuntimeException(
            "nargs must be +, ?, * or a position number: " + nargsStr);
      }
    }
    return nargsCount;
  }

  private void processArgsByAction(String arg, Arg cmdArg, ArgValue argValue) {
    // check action
    String actionName = cmdArg.action();
    Action action = Action.fromName(actionName);
    String nargsStr = cmdArg.nargs();
    String propName = cmdArg.propName();
    int nargsCount = -1;

    nargsCount = sanitycheckNargs(nargsStr);

    switch (action) {
    case STORE: {
      if (nargsStr.length() > 0 && !nargsStr.equals(NARGS_QUESTION)
          && nargsCount != 1) {
        throw new RuntimeException(
            "Action store must have nargs empty, ? or 1. nargs: " + nargsStr
                + " for flag: " + arg);
      }

      if (_followingArgs.size() == 0) {
        if (!nargsStr.equals(NARGS_QUESTION))
          throw new RuntimeException(
              "Action store must have nargs ? if no arg is passed. nargs: "
                  + nargsStr + " for flag: " + arg);
        sanityCheckVal(cmdArg, cmdArg.constant());
        argValue.setPropValue(propName, cmdArg.constant());
      } else {
        sanityCheckVal(cmdArg, _followingArgs.get(0));
        argValue.setPropValue(propName, _followingArgs.get(0));
        _followingArgs = _followingArgs.subList(1, _followingArgs.size());
      }
      break;
    }
    case STORE_CONST:
    case STORE_TRUE:
    case STORE_FALSE: {
      if (nargsStr.length() > 0)
        throw new RuntimeException(
            "Action store_const must not have narg specified. Nargs: "
                + nargsStr + ", flag: " + arg);
      if (action == Action.STORE_CONST) {
        sanityCheckVal(cmdArg, cmdArg.constant());
        argValue.setPropValue(propName, cmdArg.constant());
      } else if (action == Action.STORE_TRUE) {
        sanityCheckVal(cmdArg, "true");
        argValue.setPropValue(propName, "true");
      } else if (action == Action.STORE_FALSE) {
        sanityCheckVal(cmdArg, "false");
        argValue.setPropValue(propName, "false");
      }
      break;
    }
    case APPEND: {
      if (nargsStr.length() == 0) {
        if (_followingArgs.size() == 0)
          throw new RuntimeException(
              "narg is default, a single vlaue must be passd to flag: " + arg);
        sanityCheckVal(cmdArg, _followingArgs.get(0));
        argValue.addPropValue(propName, _followingArgs.get(0));
        _followingArgs = _followingArgs.subList(1, _followingArgs.size());
      } else if (nargsStr.equals(NARGS_QUESTION)) {
        if (_followingArgs.size() == 0) {
          sanityCheckVal(cmdArg, cmdArg.constant());
          argValue.addPropValue(propName, cmdArg.constant());
        } else {
          sanityCheckVal(cmdArg, _followingArgs.get(0));
          argValue.addPropValue(propName, _followingArgs.get(0));
          _followingArgs = _followingArgs.subList(1, _followingArgs.size());
        }
      } else if (nargsStr.equals(NARGS_PLUS)) {
        if (_followingArgs.size() == 0)
          throw new RuntimeException(
              "narg is +, at least one vlaue must be passd to flag: " + arg);
        sanityCheckVals(cmdArg, _followingArgs);
        argValue.addPropValues(propName, _followingArgs);
        _followingArgs.clear();
        ;
      } else if (nargsStr.equals(NARGS_STAR)) {
        sanityCheckVals(cmdArg, _followingArgs);
        argValue.addPropValues(propName, _followingArgs);
        _followingArgs.clear();
        ;
      } else {
        if (_followingArgs.size() < nargsCount)
          throw new RuntimeException("narg is " + nargsStr + ", exactly "
              + nargsStr + " vlaues must be passd to flag: " + arg);
        List<String> vals = _followingArgs.subList(0, nargsCount);
        sanityCheckVals(cmdArg, vals);
        argValue.addPropValues(propName, vals);
        if (_followingArgs.size() > nargsCount) {
          _followingArgs = _followingArgs.subList(nargsCount,
              _followingArgs.size());
        } else
          _followingArgs.clear();
      }
      break;
    }
    case APPEND_CONST: {
      if (nargsStr.length() != 0) {
        throw new RuntimeException(
            "Must not specify nargs for append_const action. flag: " + arg);
      }
      sanityCheckVal(cmdArg, cmdArg.constant());
      argValue.addPropValue(propName, cmdArg.constant());
      break;
    }
    case COUNT: {
      List<String> propVals = argValue.getPropValue(propName);
      if (propVals.size() > 1) {
        throw new RuntimeException(
            "append_count action but already have more than  one values. flag: "
                + arg);
      }
      int currCount;
      if (propVals.size() == 1)
        currCount = Integer.parseInt(propVals.get(0));
      else
        currCount = 0;
      String val = String.valueOf(currCount + 1);
      sanityCheckVal(cmdArg, val);
      argValue.replacePropValue(propName, val);
      break;
    }
    case HELP: {
      break;
    }
    case VERSION: {
      break;
    }
    }

    if (arg.length() > 0) {
      _positionals.addAll(_followingArgs);
      _followingArgs.clear();
    }
  }

  private void sanityCheckVal(Arg cmdArg, String val) {
    sanityCheckVals(cmdArg, Collections.singletonList(val));
  }

  private void sanityCheckVals(Arg cmdArg, List<String> vals) {
    for (String val : vals) {
      String validationType = cmdArg.validationType();
      if (validationType != null && validationType.length() > 0) {
        checkValidationType(validationType, val);
      }

      String validationRegex = cmdArg.validationRegex();
      if (validationRegex != null && validationRegex.length() > 0) {
        if (!Pattern.matches(validationRegex, val))
          throw new RuntimeException("value " + val
              + " does not matches validationRegex: " + validationRegex);
      }

      String[] choices = cmdArg.choices();
      if (choices != null && choices.length > 0) {
        boolean matched = false;
        for (String choice : choices) {
          if (choice.equals(val)) {
            matched = true;
            break;
          }
        }
        if (!matched) {
          throw new RuntimeException("val not in choices: "
              + Joiner.on(',').join(choices));
        }
      }

    }
  }

  private void checkValidationType(String validationType, String val) {
    if ("double".equalsIgnoreCase(validationType)) {
      try {
        Double.parseDouble(val);
      } catch (Exception ex) {
        usage("Value must be a double: " + val, true);
      }
    } else if ("int".equalsIgnoreCase(validationType)) {
      try {
        Integer.parseInt(val);
      } catch (Exception ex) {
        usage("Value must be an integer: " + val, true);
      }
    } else if ("boolean".equalsIgnoreCase(validationType)) {
      if (!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("false")) {
        usage("Value must be true or false: " + val, true);
      }
    } else if ("string".equalsIgnoreCase(validationType)) {

    } else
      throw new RuntimeException("Invalid validationType: " + validationType);
  }

  private void gatherArgs() {
    _argsMap = new HashMap<Integer, ArgValue>();
    _positionals = new ArrayList<String>();
    _followingArgs = new ArrayList<String>();

    _argIndex = 2;
    Set<String> passedFlags = new HashSet<String>();

    while (_argIndex < _args.length) {
      String arg = _args[_argIndex];
      _argIndex++;

      if ("--".equals(arg)) {
        // no more optional args,
        while (_argIndex < _args.length) {
          _positionals.add(_args[_argIndex]);
          _argIndex++;
        }
        break;
      }

      // check if this is a flag
      Arg cmdArg = checkFlag(arg);
      if (cmdArg != null) {
        passedFlags.add(arg);
        int paramIndex = cmdArg.index();
        ArgValue argValue = _argsMap.get(paramIndex);
        if (argValue == null) {
          argValue = new ArgValue();
          _argsMap.put(paramIndex, argValue);
        }

        while (_argIndex < _args.length) {
          String nextArg = _args[_argIndex];
          if (nextArg.equals("--")) {
            break;
          }
          Arg nextCmdArg = checkFlag(nextArg);
          if (nextCmdArg == null) {
            _followingArgs.add(nextArg);
          } else {
            break;
          }
          _argIndex++;
        }

        processArgsByAction(arg, cmdArg, argValue);
      } else {
        // positional arg
        _positionals.add(arg);
      }
    }
    
    _isHelp = false;;
    _isVersion = false;
    for (Arg arg : _cmdArgs) {
      if (arg.flags().length > 0 && arg.required()) {
        for (String flag : arg.flags()) {
          if (passedFlags.contains(flag) && arg.action().equals(Action.HELP.getName())) {
            _isHelp = true;
          }
          else if (passedFlags.contains(flag) && arg.action().equals(Action.VERSION.getName())) {
            _isVersion = true;
            _version = arg.version();
          }
        }
      }
    }
    
    if (_isHelp || _isVersion)
      return;

    // process positionals
    _followingArgs = _positionals;
    for (int index = 0; index < _positionalArgs.size(); index++) {
      Arg cmdArg = _positionalArgs.get(index);
      int paramIndex = cmdArg.index();
      ArgValue argValue = _argsMap.get(paramIndex);
      if (argValue == null) {
        argValue = new ArgValue();
        _argsMap.put(paramIndex, argValue);
      }

      processArgsByAction("", cmdArg, argValue);
    }

    if (_followingArgs.size() > 0) {
      throw new RuntimeException(
          "Still args remaining after processing all the specs, remaining count: "
              + _followingArgs.size());
    }

    // for args with no value passed, check if a defaultVal is specified.
    int paramCount = _theMethod.getParameterTypes().length;
    for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
      ArgValue argValue = _argsMap.get(paramIndex);
      if (argValue == null) {
        for (Arg cmdArg : _cmdArgs) {
          if (cmdArg.index() == paramIndex
              && (cmdArg.propName() == null || cmdArg.propName().length() == 0)
              && (cmdArg.defaultVal() != null && cmdArg.defaultVal().length() > 0)) {
            argValue = new ArgValue(Collections.singletonList(cmdArg
                .defaultVal()));
            _argsMap.put(paramIndex, argValue);
            break;
          }
        }
      }
    }

    // check if all required args are passed
    for (Arg arg : _cmdArgs) {
      if (arg.flags().length > 0 && arg.required()) {
        boolean passed = false;
        for (String flag : arg.flags()) {
          if (passedFlags.contains(flag)) {
            passed = true;
            break;
          }
        }
        if (!passed) {
          throw new RuntimeException("Required flag not passed: "
              + arg.flags()[0]);
        }
      }
    }
  }

  private Object invoke() throws NoSuchMethodException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    if (_isHelp) {
      usage(null, false);
      return "";
    }
    else if(_isVersion) {
      System.err.println(_version);
      return "";
    }
    Object obj = _clazz.getConstructor(new Class<?>[0]).newInstance();
    return _theMethod.invoke(obj, _actualArgs);
  }
}
