/*
 * Copyright  20140527 Xuelin Wang, all rights reserved.
 */

package xl.util;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import clojure.java.api.Clojure;
import clojure.lang.AFn;
import clojure.lang.IFn;
import clojure.lang.ISeq;

public class ClojureHelper
{
  public static Object invoke(String ns, String funName, Object... args)
  {
    IFn require = Clojure.var("clojure.core", "require");
    IFn seq = Clojure.var("clojure.core", "seq");
    require.invoke(Clojure.read(ns));
    IFn fun = Clojure.var(ns, funName);
    return AFn.applyToHelper(fun, (ISeq)seq.invoke(args));
  }
  
  public static void compile(String classesDir, String ns)
  {
    // make sure classDir and clojure folder are in classpath
    Object obj = invoke("xuelin.util", "compileToDir", classesDir, ns);
  }
  
  public static void compileAllFiles(String classesDir, String folderName)
      throws IOException
  {
    File dir = new File(folderName);
    if (!dir.exists())
      return;
    if (!dir.isDirectory()) {
      throw new RuntimeException("Path: " + folderName + " is not a directory");
    }
    Stack<File> files = new Stack<File>();
    files.add(dir);
    
    while (!files.isEmpty()) {
      File top = files.pop();
      if (top.isDirectory()) {
        for (File file: top.listFiles()) {
          files.push(file);
        }
        continue;
      }
      
      String fileName = top.getAbsolutePath();
      if (!fileName.endsWith(".clj"))
        continue;
      
      String startStr = "clj/src/";
      int relativeStart = fileName.indexOf(startStr);
      if (relativeStart < 0) {
        throw new RuntimeException("Compiled clojure file path must contains clj/src/: " + fileName);
      }
      
      int fromIndex = relativeStart + startStr.length();
      String relativePath = fileName.substring(fromIndex, fileName.length() - 4);
      String ns = relativePath.replace('/', '.');
      
      compile(classesDir, ns);
    }
  }
}

