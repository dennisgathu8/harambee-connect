(ns harambee.club
  "Club domain logic: profiles, squad management, club-specific queries."
  (:require [harambee.db :as db]))

;; ---------------------------------------------------------------------------
;; Club Queries
;; ---------------------------------------------------------------------------

(defn get-all-clubs
  "Get all clubs sorted by name."
  [node]
  (db/all-clubs node))

(defn get-club
  "Get a single club by ID."
  [node club-id]
  (db/entity node (keyword "club" club-id)))

(defn get-club-with-fixtures
  "Get club profile enriched with upcoming fixtures and results."
  [node club-id]
  (let [club-key (keyword "club" club-id)
        club (db/entity node club-key)
        matches (db/club-matches node club-key)]
    (when club
      (assoc club
             :club/upcoming-matches (filter #(= :upcoming (:match/status %)) matches)
             :club/recent-results (filter #(= :completed (:match/status %)) matches)
             :club/live-matches (filter #(= :live (:match/status %)) matches)))))

(defn get-rival-clubs
  "Get the rival clubs for a given club."
  [node club-id]
  (let [club (db/entity node (keyword "club" club-id))]
    (when club
      (->> (:club/rivalry club)
           (map (partial db/entity node))
           (remove nil?)))))

;; ---------------------------------------------------------------------------
;; Club Stats
;; ---------------------------------------------------------------------------

(defn club-form
  "Get a club's recent form (last 5 matches) as a string like 'WWDLW'."
  [node club-id]
  (let [club-key (keyword "club" club-id)
        matches (->> (db/club-matches node club-key)
                     (filter #(= :completed (:match/status %)))
                     (take-last 5))]
    (->> matches
         (map (fn [m]
                (let [is-home? (= club-key (:match/home m))
                      our-score (if is-home? (:match/home-score m) (:match/away-score m))
                      their-score (if is-home? (:match/away-score m) (:match/home-score m))]
                  (cond
                    (> our-score their-score) "W"
                    (= our-score their-score) "D"
                    :else "L"))))
         (apply str))))
