(ns harambee.views.standings
  "League standings table."
  (:require [harambee.i18n :refer [t]]
            [harambee.components :as c]))

(defn standings-page [state _navigate!]
  (let [standings (:standings @state)]
    [:div.container
     [:div.section-header {:style {:margin-top "16px"}}
      [:h2.section-title (t :standings/title)]]
     (if (seq standings)
       [:div.standings-table-container.animate-slide-up
        [:table.standings-table
         [:thead
          [:tr
           [:th (t :standings/position)]
           [:th (t :standings/team)]
           [:th (t :standings/played)]
           [:th (t :standings/won)]
           [:th (t :standings/drawn)]
           [:th (t :standings/lost)]
           [:th (t :standings/goals-for)]
           [:th (t :standings/goals-against)]
           [:th (t :standings/goal-diff)]
           [:th (t :standings/points)]]]
         [:tbody
          (for [team standings]
            ^{:key (:club-id team)}
            [:tr
             [:td
              [:span.standings-position (:position team)]]
             [:td
              [:div.team-cell
               [:span.team-cell-badge (:club-emoji team)]
               [:span (:club-short team)]]]
             [:td (or (:played team) 0)]
             [:td (or (:won team) 0)]
             [:td (or (:drawn team) 0)]
             [:td (or (:lost team) 0)]
             [:td (or (:gf team) 0)]
             [:td (or (:ga team) 0)]
             [:td (or (:gd team) 0)]
             [:td (or (:points team) 0)]])]]]
       [c/loading-spinner])]))
