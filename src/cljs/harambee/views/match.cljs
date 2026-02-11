(ns harambee.views.match
  "Live Match Centre: real-time score, events timeline, lineups."
  (:require [harambee.i18n :refer [t]]
            [harambee.components :as c]))

(defn match-events-timeline [events]
  (when (seq events)
    [:div {:style {:margin-bottom "24px"}}
     [:div.section-header
      [:h2.section-title (t :match/events)]]
     [:div.events-timeline
      (for [[idx event] (map-indexed vector (reverse events))]
        ^{:key idx}
        [c/event-item {:event event}])]]))

(defn match-lineups [lineups home-club away-club]
  (when (and lineups
             (or (seq (:home lineups)) (seq (:away lineups))))
    [:div {:style {:margin-bottom "24px"}}
     [:div.section-header
      [:h2.section-title (t :match/lineup)]]
     [:div.lineup-grid
      [:div.lineup-team
       [:h3 (or (:club/short-name home-club) "Home")]
       (for [player (:home lineups)]
         ^{:key (:player/number player)}
         [:div.lineup-player
          [:span.player-number (:player/number player)]
          [:span (:player/name player)]
          [:span.player-position (:player/position player)]])]
      [:div.lineup-team
       [:h3 (or (:club/short-name away-club) "Away")]
       (for [player (:away lineups)]
         ^{:key (:player/number player)}
         [:div.lineup-player
          [:span.player-number (:player/number player)]
          [:span (:player/name player)]
          [:span.player-position (:player/position player)]])]]]))

(defn match-detail-page [state navigate!]
  (let [match-id (:selected-match @state)
        matches (:matches @state)
        match (first (filter #(= (str (:xt/id %)) (str match-id)) matches))]
    (if match
      (let [home-club (:match/home-club match)
            away-club (:match/away-club match)
            status (keyword (name (:match/status match)))]
        [:div.container.animate-fade-in
         [c/back-button #(navigate! :home)]
         [:div.match-detail-header
          [c/match-status-badge status]
          [:div.match-teams {:style {:margin-top "16px"}}
           [:div.match-team
            [:div.team-badge {:style {:font-size "2.5rem"
                                      :width "64px"
                                      :height "64px"}}
             (or (:club/badge-emoji home-club) "🏟️")]
            [:span.team-name {:style {:font-size "0.95rem"}}
             (or (:club/name home-club) "Home")]]
           [:div.match-detail-score
            [:div.match-score
             [:span.score-number {:style {:font-size "3rem"}}
              (or (:match/home-score match) 0)]
             [:span.score-separator {:style {:font-size "1.5rem"}} "—"]
             [:span.score-number {:style {:font-size "3rem"}}
              (or (:match/away-score match) 0)]]]
           [:div.match-team
            [:div.team-badge {:style {:font-size "2.5rem"
                                      :width "64px"
                                      :height "64px"}}
             (or (:club/badge-emoji away-club) "🏟️")]
            [:span.team-name {:style {:font-size "0.95rem"}}
             (or (:club/name away-club) "Away")]]]
          [:div.match-minute {:class (when (= status :live) "live")
                              :style {:font-size "1rem" :margin-top "8px"}}
           (case status
             :live (str (t :match/minute) " " (:match/minute match) "'")
             :completed (t :match/completed)
             (:match/date match))]
          [:div.match-info
           (when (:match/venue match)
             [:span (str "📍 " (:match/venue match))])
           (when (:match/referee match)
             [:span (str "👤 " (:match/referee match))])
           (when (:match/attendance match)
             [:span (str "👥 " (:match/attendance match))])]]

         ;; Tabs
         [:div.tabs
          [:div.tab.active (t :match/events)]
          [:div.tab (t :match/lineup)]]

         ;; Events Timeline
         [match-events-timeline (:match/events match)]

         ;; Lineups
         [match-lineups (:match/lineups match) home-club away-club]])

      [c/empty-state "⚽" (t :common/loading)])))

(defn matches-page [state navigate!]
  (let [matches (:matches @state)]
    [:div.container
     [:div.section-header {:style {:margin-top "16px"}}
      [:h2.section-title (t :nav/matches)]]
     (if (seq matches)
       [:div.fixtures-list.animate-slide-up
        (for [match matches]
          (let [home-club (:match/home-club match)
                away-club (:match/away-club match)
                status (keyword (name (:match/status match)))]
            ^{:key (:xt/id match)}
            [:div.fixture-item {:on-click #(navigate! :match-detail (:xt/id match))}
             [:div {:style {:display "flex" :align-items "center" :gap "8px"}}
              [c/match-status-badge status]
              [:div.fixture-teams
               [:span (or (:club/badge-emoji home-club) "🏟️")]
               [:span (or (:club/short-name home-club) "Home")]
               [:span.fixture-vs (str " " (:match/home-score match) " - " (:match/away-score match) " ")]
               [:span (or (:club/badge-emoji away-club) "🏟️")]
               [:span (or (:club/short-name away-club) "Away")]]]
             [:div.fixture-meta
              [:div (:match/venue match)]]]))]
       [c/loading-spinner])]))
