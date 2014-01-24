(ns schemad.handler
  (:require [clojure.java.io :as io]
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

; Page to input schema details and upload schema ------------------------------
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

;; Page to handle schema upload ------------------------------------------------
(defn handle-upload
  [{:keys [file schema-name schema-description]}]
  (let [id (java.util.UUID/randomUUID)
        temp (java.io.File. (format "resources/temp/%s.edn" id))
        _ (io/copy (:tempfile file) temp)]
    (try
      (let [check-s #(if (empty? (.trim %)) "Unknown" (.trim %))
            sname (check-s schema-name)
            sdesc (check-s schema-description)
            report (parsed-schema->html
                    (core/parse-schema (core/file->schema temp)) sname sdesc)
            output #(spit (format "resources/public/reports/%s.%s" id %1) %2)]
        (doseq [[postfix content] [["n" sname] ["d" sdesc] ["html" html-report]]]
          (output postfix content))
        (response/redirect-after-post (format "/public/reports/%s.html" id)))
      (catch Exception e
        (response/status {:body (new-schema "Could not parse the schema")} 400))
      (finally (when (.exists temp) (.delete temp))))))

;; Routes and app -------------------------------------------------------------
(defroutes app-routes
  (GET "/" [] (new-schema))
  (POST "/schemas" req (handle-upload (:params req)))
  (route/resources "/public" :root "resources")
  (route/not-found "Not Found"))

(def app (handler/site app-routes))
