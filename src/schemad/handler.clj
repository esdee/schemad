(ns schemad.handler
  (:require [clojure.java.io :as io]
            [datomic.api :as d :refer (q db)]
            [compojure.core :refer (defroutes GET POST)]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :refer (run-jetty)]
            [ring.util.response :as response]
            [hiccup.core :refer (html)]
            [hiccup.form :as form]
            [hiccup.page :refer (include-css)]
            [schemad.core :as core]
            [schemad.reports :refer (parsed-schema->html)]))

(defn new-schema
  [& [errors]]
  (html
    [:head (include-css "/public/css/bootstrap.min.css")]
    [:body
     [:div.content
      [:div#errors errors]
      (form/form-to
        {:enctype "multipart/form-data" :role "form"}
        [:post "/schemas"]
        [:div.form-group
         [:label {:for "schemaName"} "Name"]
         [:input#schemaName.form-control {:type "text" :name "schema-name"}]]
        [:div.form-group
         [:label {:for "schemaDesc"} "Description"]
         [:input#schemaDescription.form-control {:type "text"
                                                 :name "schema-description"}]]
        (form/file-upload :file)
        (form/submit-button "upload"))]]))

(defn handle-upload
  [{:keys [file schema-name schema-description]}]
  (let [id (java.util.UUID/randomUUID)
        temp (java.io.File. (format "resources/temp/%s.edn" id))
        _ (io/copy (:tempfile file) temp)]
    (try
      (let [check-s #(if (empty? (.trim %)) "Unknown" (.trim %))
            sname (check-s schema-name)
            sdesc (chec-s schema-description)
            html-report (parsed-schema->html
                          (core/parse-schema
                            (core/text->schema
                              (slurp temp)))
                          sname
                          sdesc)
            ]
        (do
            (when (.exists temp) (.delete temp))
           ;(spit (format "resources/public/reports/%s.n" (check-s schema-name)))
           ;(spit (format "resources/public/reports/%s.d" (check-s schema-description)))
           ;(spit (format "resources/public/reports/%s.html" html-report))
            (response/redirect-after-post (format "/public/reports/%s.html"))))
      (catch Exception e
        (throw e)
        (response/status (new-schema "Could not parse the schema") 400)))))

(defroutes app-routes
  (GET "/" [] (new-schema))
  (POST "/schemas" req (handle-upload (:params req)))
  (route/resources "/public" :root "resources")
  (route/not-found "Not Found"))

(def app (handler/site app-routes))


