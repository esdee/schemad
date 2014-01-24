(ns schemad.core
  (:require [datomic.api :as d :refer (q db)]))

(defn- system-identity?
  "Returns true if this identity field used for system entities"
  [identity]
  (re-seq #"^\:db[\.*|\/]" (str identity)))

(defn- system-entity?
  "Returns true if this entity is part of the datomic sytem entity set"
  [[_ identity & _]]
  (or (system-identity? identity)
      (re-seq #"^\:fressian" (str identity))))

(defn get-user-entities
  "Return a seq of the user entitites in the database.
   User entities are non system entities.
   A user entity consisst of a map with the keys:
   :db/id :db/ident :db/valueType :db/cardinality :db/doc"
  [db]
  (let [all (q '[:find ?a ?attr ?type ?card ?doc
                 :where
                 [_ :db.install/attribute ?a]
                 [?a :db/valueType ?t]
                 [?a :db/cardinality ?c]
                 [?a :db/doc ?doc]
                 [?a :db/ident ?attr]
                 [?t :db/ident ?type]
                 [?c :db/ident ?card]]
               db)]
    (->> all
         (remove system-entity?)
         (map #(list %1 %2)
              (repeat [:db/id :db/ident :db/valueType :db/cardinality :db/doc]))
         (map (fn [[ks vs]] (zipmap ks vs))))))

(defn get-enums
  "Return a seq of the enums in the database.
   A enum is a map with the following keys:
   :db/id :db/identity"
  [db]
  (->> (q '[:find ?e :where [?e :db/ident _]] db)
       (map first)
       (map #(d/entity db %))
       (map d/touch)
       (remove :db/valueType)
       (remove #(system-identity? (:db/ident %)))))

(defn- identity-grouping-fn
  [{id :db/ident}]
  (let [group-id (str id)]
    (.substring group-id 0 (.indexOf group-id "/"))))

(defn group-entities
  "Group entities based on the root entitt they belong to.
  For example :customer/firstName, :customer/lastName would be grouped"
  [entities]
  (group-by identity-grouping-fn entities))

(defn group-enums
  "Group enums by their root. For example,
  :customer.status/dead, :customer.status/alive would be grouped together"
  [enums]
  (group-by identity-grouping-fn enums))

(defn parse-schema
  "Given a schema as clojure data, return a map with the keys:
   :entities and :enums"
  [schema]
  (let [uri (str "datomic:mem://" (java.util.UUID/randomUUID))]
    (try
      (d/create-database uri)
      (let [conn (d/connect uri)
            _ @(d/transact conn schema)
            dbase (db conn)]
        {:entities (group-entities (get-user-entities dbase))
         :enums (group-enums (get-enums dbase))})
      (finally
        (d/delete-database uri)))))

(defn text->schema
  [text]
  (binding [*read-eval* false]
    (read-string text))

(defn file->schema
  [file]
  (text->schema
    (slurp file)))
