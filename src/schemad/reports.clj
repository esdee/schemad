(ns schemad.reports
  (:require [hiccup.core :refer (html)])
  )

(defn parsed-schema->html
  [parsed-schema schema-name schema-description]
  (html
    [:head]
    [:body
     [:h2 "Hello World"]
     [:span schema-name]
     [:span schema-description]
     ]))


