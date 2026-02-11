(ns harambee.views.clubs
  "Club listing and club detail pages."
  (:require [harambee.i18n :refer [t]]
            [harambee.components :as c]))

(defn club-detail-page [state navigate!]
  (let [club-id (:selected-club @state)
        clubs (:clubs @state)
        club (first (filter #(= (str (:xt/id %)) (str club-id)) clubs))]
    (if club
      [:div.container.animate-fade-in
       [c/back-button #(navigate! :clubs)]
       [:div.club-detail-header
        [:div.club-detail-badge (:club/badge-emoji club)]
        [:h1.club-detail-name (:club/name club)]
        [:p.club-detail-nickname (str "\"" (:club/nickname club) "\"")]
        [:div.club-meta-grid
         [:div.club-meta-item
          [:div.club-meta-label (t :club/city)]
          [:div.club-meta-value (:club/city club)]]
         [:div.club-meta-item
          [:div.club-meta-label (t :club/founded)]
          [:div.club-meta-value (:club/founded club)]]
         [:div.club-meta-item
          [:div.club-meta-label (t :club/stadium)]
          [:div.club-meta-value (:club/stadium club)]]
         [:div.club-meta-item
          [:div.club-meta-label (t :club/nickname)]
          [:div.club-meta-value (:club/nickname club)]]]]
       (when (:club/description club)
         [:div {:style {:background "var(--bg-card)"
                        :border "1px solid var(--border-subtle)"
                        :border-radius "var(--radius-lg)"
                        :padding "var(--space-lg)"
                        :margin-bottom "var(--space-lg)"
                        :font-size "0.9rem"
                        :color "var(--text-secondary)"
                        :line-height "1.7"}}
          (:club/description club)])]
      [c/empty-state "🏟️" (t :common/loading)])))

(defn clubs-page [state navigate!]
  (let [clubs (:clubs @state)]
    [:div.container
     [:div.section-header {:style {:margin-top "16px"}}
      [:h2.section-title (t :nav/clubs)]]
     (if (seq clubs)
       [:div.clubs-grid.animate-slide-up
        (for [club clubs]
          ^{:key (:xt/id club)}
          [:div.club-card {:on-click #(navigate! :club-detail (:xt/id club))}
           [:div.club-card-badge (:club/badge-emoji club)]
           [:div.club-card-name (:club/name club)]
           [:div.club-card-nickname (:club/nickname club)]
           [:div.club-card-city "📍 " (:club/city club)]])]
       [c/loading-spinner])]))
