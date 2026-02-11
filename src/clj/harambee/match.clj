(ns harambee.match
  "Match domain logic: live state management, event recording, standings."
  (:require [harambee.db :as db]
            [harambee.sse :as sse]
            [clojure.tools.logging :as log]
            [xtdb.api :as xt]))

;; ---------------------------------------------------------------------------
;; Match Queries
;; ---------------------------------------------------------------------------

(defn get-all-matches
  "Get all matches, optionally filtered by status."
  ([node] (db/all-matches node))
  ([node status] (db/matches-by-status node status)))

(defn get-match
  "Get a single match by ID."
  [node match-id]
  (db/entity node (keyword "match" match-id)))

(defn enrich-match
  "Enrich a match with full club data."
  [node match]
  (when match
    (let [home-club (db/entity node (:match/home match))
          away-club (db/entity node (:match/away match))]
      (assoc match
             :match/home-club home-club
             :match/away-club away-club))))

(defn get-enriched-matches
  "Get all matches enriched with club data."
  ([node]
   (->> (db/all-matches node)
        (map (partial enrich-match node))))
  ([node status]
   (->> (db/matches-by-status node status)
        (map (partial enrich-match node)))))

;; ---------------------------------------------------------------------------
;; Match Event Recording
;; ---------------------------------------------------------------------------

(defn record-event!
  "Record a match event (goal, card, substitution) and broadcast via SSE."
  [node match-id event]
  (let [match (db/entity node match-id)
        updated-events (conj (or (:match/events match) []) event)
        ;; Update score if goal
        updated-match (cond-> (assoc match :match/events updated-events)
                        (= :goal (:event/type event))
                        (update (if (= (:event/team event) (:match/home match))
                                  :match/home-score
                                  :match/away-score)
                                inc))]
    ;; Persist to XTDB
    (db/put! node updated-match)
    ;; Broadcast via SSE
    (sse/publish-event! {:type (:event/type event)
                         :match-id match-id
                         :data (merge event
                                      {:home-score (:match/home-score updated-match)
                                       :away-score (:match/away-score updated-match)
                                       :minute (:match/minute updated-match)})})
    (log/info "Recorded event:" (:event/type event) "for match" match-id)
    updated-match))

(defn update-minute!
  "Update the current minute of a live match."
  [node match-id minute]
  (let [match (db/entity node match-id)
        updated (assoc match :match/minute minute)]
    (db/put! node updated)
    (sse/publish-event! {:type :minute-update
                         :match-id match-id
                         :data {:minute minute}})
    updated))

(defn update-status!
  "Update match status (e.g., :upcoming -> :live -> :completed)."
  [node match-id status]
  (let [match (db/entity node match-id)
        updated (assoc match :match/status status)]
    (db/put! node updated)
    (sse/publish-event! {:type :status-change
                         :match-id match-id
                         :data {:status status}})
    (log/info "Match" match-id "status changed to" status)
    updated))

;; ---------------------------------------------------------------------------
;; Standings Computation
;; ---------------------------------------------------------------------------

(defn compute-standings
  "Compute league standings from completed and live matches.
   Returns sorted list of team records."
  [node]
  (let [clubs (db/all-clubs node)
        matches (->> (db/all-matches node)
                     (filter #(#{:completed :live} (:match/status %))))
        ;; Build standings map
        standings-map
        (reduce
         (fn [acc match]
           (let [home (:match/home match)
                 away (:match/away match)
                 hs (:match/home-score match)
                 as (:match/away-score match)
                 update-team (fn [m team gf ga]
                               (-> m
                                   (update-in [team :played] (fnil inc 0))
                                   (update-in [team :gf] (fnil + 0) gf)
                                   (update-in [team :ga] (fnil + 0) ga)
                                   (cond->
                                     (> gf ga) (-> (update-in [team :won] (fnil inc 0))
                                                   (update-in [team :points] (fnil + 0) 3))
                                     (= gf ga) (-> (update-in [team :drawn] (fnil inc 0))
                                                   (update-in [team :points] (fnil + 0) 1))
                                     (< gf ga) (update-in [team :lost] (fnil inc 0)))))]
             (-> acc
                 (update-team home hs as)
                 (update-team away as hs))))
         {}
         matches)]
    (->> clubs
         (map (fn [club]
                (let [id (:xt/id club)
                      stats (get standings-map id {})
                      played (or (:played stats) 0)
                      won    (or (:won stats) 0)
                      drawn  (or (:drawn stats) 0)
                      lost   (or (:lost stats) 0)
                      gf     (or (:gf stats) 0)
                      ga     (or (:ga stats) 0)
                      pts    (or (:points stats) 0)
                      gd     (- gf ga)]
                  {:club-id   (name id)
                   :club-name  (:club/name club)
                   :club-short (:club/short-name club)
                   :club-emoji (:club/badge-emoji club)
                   :played played :won won :drawn drawn :lost lost
                   :gf gf :ga ga :gd gd :points pts})))
         (sort-by (juxt (comp - :points) (comp - :gd) (comp - :gf)))
         (map-indexed (fn [idx team] (assoc team :position (inc idx)))))))
