(ns harambee.db
  "XTDB database setup with seed data for the FKF Premier League.
   Uses in-memory node for development, with immutable bitemporal storage."
  (:require [xtdb.api :as xt]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]))

;; ---------------------------------------------------------------------------
;; XTDB Node Lifecycle
;; ---------------------------------------------------------------------------

(defn start-node
  "Start an in-memory XTDB node."
  []
  (log/info "Starting XTDB in-memory node...")
  (xt/start-node {}))

(defn stop-node
  "Stop the XTDB node."
  [node]
  (log/info "Stopping XTDB node...")
  (.close node))

(defstate xtdb-node
  :start (start-node)
  :stop (stop-node xtdb-node))

;; ---------------------------------------------------------------------------
;; Data Loading
;; ---------------------------------------------------------------------------

(defn load-edn
  "Load an EDN file from the classpath resources."
  [path]
  (when-let [resource (io/resource path)]
    (edn/read-string (slurp resource))))

(defn seed-data!
  "Seed the database with clubs and fixtures from EDN data files."
  [node]
  (log/info "Seeding database with FKF Premier League data...")
  (let [clubs (load-edn "data/clubs.edn")
        fixtures (load-edn "data/fixtures.edn")]
    ;; Submit clubs
    (when clubs
      (let [tx-ops (mapv (fn [club] [::xt/put club]) clubs)]
        (xt/submit-tx node tx-ops)
        (log/info (str "Seeded " (count clubs) " clubs"))))
    ;; Submit fixtures
    (when fixtures
      (let [tx-ops (mapv (fn [fixture] [::xt/put fixture]) fixtures)]
        (xt/submit-tx node tx-ops)
        (log/info (str "Seeded " (count fixtures) " fixtures"))))
    ;; Wait for indexing
    (xt/sync node)
    (log/info "Database seeding complete.")))

;; ---------------------------------------------------------------------------
;; Query Helpers
;; ---------------------------------------------------------------------------

(defn entity
  "Fetch a single entity by ID."
  [node id]
  (xt/entity (xt/db node) id))

(defn query
  "Execute an XTDB Datalog query."
  [node q & args]
  (apply xt/q (xt/db node) q args))

(defn put!
  "Put a document into XTDB."
  [node doc]
  (xt/submit-tx node [[::xt/put doc]])
  (xt/sync node))

(defn all-clubs
  "Fetch all clubs from the database."
  [node]
  (let [results (query node
                       '{:find [(pull ?e [*])]
                         :where [[?e :club/name]]})]
    (->> results
         (map first)
         (sort-by :club/name))))

(defn all-matches
  "Fetch all matches from the database."
  [node]
  (let [results (query node
                       '{:find [(pull ?e [*])]
                         :where [[?e :match/home]]})]
    (->> results
         (map first)
         (sort-by :match/date))))

(defn matches-by-status
  "Fetch matches filtered by status (:live, :completed, :upcoming)."
  [node status]
  (let [results (query node
                       '{:find [(pull ?e [*])]
                         :where [[?e :match/status status]]
                         :in [status]}
                       status)]
    (->> results
         (map first)
         (sort-by :match/date))))

(defn club-matches
  "Fetch all matches for a specific club."
  [node club-id]
  (let [results (query node
                       '{:find [(pull ?e [*])]
                         :where [(or [?e :match/home club-id]
                                     [?e :match/away club-id])]
                         :in [club-id]}
                       club-id)]
    (->> results
         (map first)
         (sort-by :match/date))))
