(ns harambee.components
  "Shared UI components for Harambee Stars Connect."
  (:require [harambee.i18n :as i18n :refer [t]]
            [harambee.offline :as offline]))

;; ---------------------------------------------------------------------------
;; Header
;; ---------------------------------------------------------------------------

(defn header []
  [:header.header
   [:div.header-logo
    [:span.header-logo-icon "⚽"]
    [:span.header-logo-text "Harambee Connect"]]
   [:div.header-actions
    (when-not @offline/online?
      [:span.offline-badge (t :offline/indicator)])
    [:button.lang-toggle
     {:on-click i18n/toggle-lang!}
     (t :common/language)]]])

;; ---------------------------------------------------------------------------
;; Bottom Navigation
;; ---------------------------------------------------------------------------

(defn navbar [current-page navigate!]
  [:nav.navbar
   [:ul.nav-items
    [:li.nav-item {:class (when (= current-page :home) "active")
                   :on-click #(navigate! :home)}
     [:span.nav-icon "🏠"]
     [:span.nav-label (t :nav/home)]]
    [:li.nav-item {:class (when (= current-page :matches) "active")
                   :on-click #(navigate! :matches)}
     [:span.nav-icon "⚽"]
     [:span.nav-label (t :nav/matches)]]
    [:li.nav-item {:class (when (= current-page :clubs) "active")
                   :on-click #(navigate! :clubs)}
     [:span.nav-icon "🏟️"]
     [:span.nav-label (t :nav/clubs)]]
    [:li.nav-item {:class (when (= current-page :standings) "active")
                   :on-click #(navigate! :standings)}
     [:span.nav-icon "📊"]
     [:span.nav-label (t :nav/standings)]]
    [:li.nav-item {:class (when (= current-page :tickets) "active")
                   :on-click #(navigate! :tickets)}
     [:span.nav-icon "🎫"]
     [:span.nav-label (t :nav/tickets)]]]])

;; ---------------------------------------------------------------------------
;; Match Card
;; ---------------------------------------------------------------------------

(defn match-status-badge [status]
  (let [status-key (keyword status)
        label (case status-key
                :live (t :match/live)
                :completed (t :match/completed)
                :upcoming (t :match/upcoming)
                (name status))]
    [:span.match-status-badge {:class (name status-key)}
     (when (= status-key :live)
       [:span.pulse-dot])
     label]))

(defn match-card [{:keys [match on-click]}]
  (let [status (keyword (name (:match/status match)))
        home-club (:match/home-club match)
        away-club (:match/away-club match)]
    [:div.match-card {:class (name status)
                      :on-click on-click}
     [match-status-badge status]
     [:div.match-teams
      [:div.match-team
       [:div.team-badge (or (:club/badge-emoji home-club) "🏟️")]
       [:span.team-name (or (:club/short-name home-club)
                            (:club/name home-club)
                            "Home")]]
      [:div.match-score
       [:span.score-number (or (:match/home-score match) 0)]
       [:span.score-separator "—"]
       [:span.score-number (or (:match/away-score match) 0)]]
      [:div.match-team
       [:div.team-badge (or (:club/badge-emoji away-club) "🏟️")]
       [:span.team-name (or (:club/short-name away-club)
                            (:club/name away-club)
                            "Away")]]]
     [:div.match-minute {:class (when (= status :live) "live")}
      (case status
        :live (str (t :match/minute) " " (:match/minute match) "'")
        :completed (t :match/completed)
        :upcoming (:match/date match)
        "")]
     [:div.match-info
      (when (:match/venue match)
        [:span (str "📍 " (:match/venue match))])]]))

;; ---------------------------------------------------------------------------
;; Event Item
;; ---------------------------------------------------------------------------

(defn event-icon [event-type]
  (case (keyword (name event-type))
    :goal "⚽"
    :yellow-card "🟨"
    :red-card "🟥"
    :substitution "🔄"
    "📋"))

(defn event-item [{:keys [event]}]
  (let [etype (keyword (name (:event/type event)))]
    [:div.event-item {:class (name etype)}
     [:div.event-minute (str (:event/minute event) "'")]
     [:div.event-content
      [:span.event-type-icon (event-icon etype)]
      [:span.event-player (or (:event/player event)
                              (:event/player-in event))]
      (when (:event/detail event)
        [:div.event-detail (:event/detail event)])
      (when (:event/player-out event)
        [:div.event-detail (str "↩ " (:event/player-out event))])]]))

;; ---------------------------------------------------------------------------
;; Loading & Empty States
;; ---------------------------------------------------------------------------

(defn loading-spinner []
  [:div.empty-state
   [:div.loading-spinner
    [:div.spinner]]
   [:p.loading-text (t :common/loading)]])

(defn empty-state [icon text]
  [:div.empty-state
   [:div.empty-state-icon icon]
   [:p.empty-state-text text]])

;; ---------------------------------------------------------------------------
;; Back Button
;; ---------------------------------------------------------------------------

(defn back-button [on-click]
  [:button.back-btn {:on-click on-click}
   "← " (t :common/back)])
