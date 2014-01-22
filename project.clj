(defproject schemad "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.9.4384"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [hiccup "1.0.4"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler schemad.handler/app}
  :profiles
    {:dev {:dependencies [[midje "1.6.0"]
                          [javax.servlet/servlet-api "2.5"]]}})
