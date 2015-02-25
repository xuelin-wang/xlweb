(ns ^{:doc
      "wendocs webapp"
      :author "xl"}
  xl.wendocs)

(defn wendocsEcho
  "echo"
  [first & rest]
  (print first rest)
)

(defn acl
  "Check acl"
  [userId]
  true
)

(defn formatStr
  "just change str a bit"
  [& strs]
  (apply str "changed " strs)
)


(defn formatStr2
  "just change str a bit more"
  [& strs]
  {"str" (apply str "changed " strs)}
)

