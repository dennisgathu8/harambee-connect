(ns harambee.views.home
  "Home page: hero section, live matches carousel, upcoming fixtures."
  (:require [harambee.i18n :refer [t]]
            [harambee.components :as c]))

(defn hero-section []
  [:div.hero.animate-fade-in
   [:h1.hero-title
    [:span.gradient-text (t :home/title)]
    [:br]
    (t :home/subtitle)]
   [:p.hero-subtitle (t :home/hero-text)]])

(defn live-matches-section [matches navigate!]
  (let [live (filter #(= :live (keyword (name (:match/status %)))) matches)]
    (when (seq live)
      [:div.animate-slide-up {:style {:margin-bottom "24px"}}
       [:div.section-header
        [:h2.section-title (t :home/live-matches)]
        [:a.section-link {:on-click #(navigate! :matches)}
         (t :common/see-all) " →"]]
       [:div.live-matches-scroll
        (for [match live]
          ^{:key (:xt/id match)}
          [c/match-card {:match match
                         :on-click #(navigate! :match-detail (:xt/id match))}])]])))

(defn upcoming-matches-section [matches navigate!]
  (let [upcoming (filter #(= :upcoming (keyword (name (:match/status %)))) matches)]
    (when (seq upcoming)
      [:div.animate-slide-up {:style {:margin-bottom "24px"}}
       [:div.section-header
        [:h2.section-title (t :home/upcoming)]]
       [:div.fixtures-list
        (for [match upcoming]
          (let [home-club (:match/home-club match)
                away-club (:match/away-club match)]
            ^{:key (:xt/id match)}
            [:div.fixture-item {:on-click #(navigate! :match-detail (:xt/id match))}
             [:div.fixture-teams
              [:span (or (:club/badge-emoji home-club) "🏟️")]
              [:span (or (:club/short-name home-club) "Home")]
              [:span.fixture-vs (str " " (t :match/vs) " ")]
              [:span (or (:club/badge-emoji away-club) "🏟️")]
              [:span (or (:club/short-name away-club) "Away")]]
             [:div.fixture-meta
              [:div (:match/venue match)]
              [:div (:match/date match)]]]))]])))

(defn results-section [matches navigate!]
  (let [completed (filter #(= :completed (keyword (name (:match/status %)))) matches)]
    (when (seq completed)
      [:div.animate-slide-up {:style {:margin-bottom "24px"}}
       [:div.section-header
        [:h2.section-title (t :home/results)]]
       [:div.live-matches-scroll
        (for [match completed]
          ^{:key (:xt/id match)}
          [c/match-card {:match match
                         :on-click #(navigate! :match-detail (:xt/id match))}])]])))

(defn home-page [state navigate!]
  (let [matches (:matches @state)]
    [:div.container
     [hero-section]
     (if (seq matches)
       [:<>
        [live-matches-section matches navigate!]
        [upcoming-matches-section matches navigate!]
        [results-section matches navigate!]]
       [c/loading-spinner])]))
