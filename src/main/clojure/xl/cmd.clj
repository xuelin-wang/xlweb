(ns ^{:doc
      "command processing"
      :author "xl"}
  xl.cmd)

(defrecord JavaCmd [className methodName args])
(defrecord JavaArgSpec [index flags action dest nargs])
(defrecord CmdSpec [argSpecs])

(defn getArgs [CmdSpec strArgs]
  nil
)