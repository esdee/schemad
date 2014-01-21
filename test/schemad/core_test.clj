(ns schemad.core-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [datomic.api :as d]
            [schemad.core :as core]))

;; Helper fns ---------------------------------------------------------------------
(defn recreate-db
  [schema]
  (let [uri "datomic:mem://schemad-tests"
        _ (d/delete-database uri)
        _ (d/create-database uri)
        conn (d/connect uri)
        _  @(d/transact conn schema)]
    (d/db conn)))

(defn- eval-schema
  [file]
  (binding [*read-eval* false]
    (read-string
     (slurp (str "resources/" file ".edn")))))

(defn- schema->seq
  [schema]
  (->> schema
       (map #(select-keys % [:db/ident :db/valueType :db/cardinality :db/doc]))
       (filter seq)))

(defn- without-id
  [entities]
  (map #(dissoc % :db/id) entities))

;; Expected Data -------------------------------------------------------------------
(fact
 "get-entities should return just the user entitities for a database"
 ; Credit Card schema
 (let [schema (eval-schema "credit-card-schema")
       db (recreate-db schema)]
   (without-id (core/get-user-entities db))
   => (just (schema->seq schema) :in-any-order))
 ; Seattle schema
 (let [schema (eval-schema "seattle-schema")
       db (recreate-db schema)]
   (without-id (core/get-user-entities db))
   => (just (schema->seq schema) :in-any-order))
 ; Music Brainz schema
 (let [schema (eval-schema "mbrainz-schema")
       db (recreate-db schema)]
   (without-id (core/get-user-entities db))
   => (just (schema->seq schema) :in-any-order)))

(fact
 "get-enums should return the enums for a database"
 (let [schema (eval-schema "credit-card-schema")
       db (recreate-db schema)]
   (map :db/ident (core/get-enums db))
   => (just [:creditCardCharge.status/failed
             :creditCardCharge.status/succeeded
             :creditCardCharge.status/disputed]
            :in-any-order)))

(fact
 "group-entities should take a seq of entities {:db/identity :custmomer/firstName, :db.valueType string ...}
 and return a map where each key is the db entity 'customer' and each value is a seq of the  attributes"
 (let [grouped-entities (core/group-entities
                         (schema->seq (eval-schema "credit-card-schema")))]
   (keys grouped-entities) => (just [":customer" ":creditCardCharge" ":currency"]
                                    :in-any-order)
   (grouped-entities ":customer") => (just [{:db/cardinality :db.cardinality/one
                                            :db/doc "A customer's first name"
                                            :db/ident :customer/firstName
                                            :db/valueType :db.type/string}
                                           {:db/cardinality :db.cardinality/one
                                            :db/doc "A customer's last name"
                                            :db/ident :customer/lastName
                                            :db/valueType :db.type/string}]
                                          :in-any-order)
   (grouped-entities ":creditCardCharge") => (just [{:db/cardinality :db.cardinality/one
                                                    :db/doc "Unix timestamp for the creation date of the charge"
                                                    :db/ident :creditCardCharge/createdAt
                                                    :db/valueType :db.type/long}
                                                   {:db/cardinality :db.cardinality/one
                                                    :db/doc "Set to true if the charge has been paid"
                                                    :db/ident :creditCardCharge/isPaid
                                                    :db/valueType :db.type/boolean}
                                                   {:db/cardinality :db.cardinality/one
                                                    :db/doc "Set to true if the charge has been refunded"
                                                    :db/ident :creditCardCharge/isRefunded
                                                    :db/valueType :db.type/boolean}
                                                   {:db/cardinality :db.cardinality/one
                                                    :db/doc "Charge amount in minimal countries equivalent of cents"
                                                    :db/ident :creditCardCharge/amount
                                                    :db/valueType :db.type/long}
                                                   {:db/cardinality :db.cardinality/one
                                                    :db/doc "Currency that the charge is made in"
                                                    :db/ident :creditCardCharge/currency
                                                    :db/valueType :db.type/ref}
                                                   {:db/cardinality :db.cardinality/one
                                                    :db/doc "Customer to apply the charge to"
                                                    :db/ident :creditCardCharge/customer
                                                    :db/valueType :db.type/ref}
                                                   {:db/cardinality :db.cardinality/one
                                                    :db/doc "What is the current status of the charge - successful, failed, disputed"
                                                    :db/ident :creditCardCharge/status
                                                    :db/valueType :db.type/ref}]
                                                  :in-any-order)
   (grouped-entities ":currency") => (just [{:db/cardinality :db.cardinality/one
                                            :db/doc "Currency name"
                                            :db/ident :currency/name
                                            :db/valueType :db.type/string}
                                           {:db/cardinality :db.cardinality/one
                                            :db/doc "Currency common symbol e.g. $",
                                            :db/ident :currency/symbol
                                            :db/valueType :db.type/string}]
                                          :in-any-order)

   ))