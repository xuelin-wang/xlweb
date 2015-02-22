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
  [st]
  (str "changed " st)
)


(defn formatStr2
  "just change str a bit more"
  [st]
  {"str" (str "changed " st)}
)

