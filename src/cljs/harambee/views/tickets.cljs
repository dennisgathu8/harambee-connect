(ns harambee.views.tickets
  "M-Pesa ticket purchase flow."
  (:require [reagent.core :as r]
            [harambee.i18n :refer [t]]
            [harambee.components :as c]
            [harambee.offline :as offline]))

(defn tickets-page [state navigate!]
  (let [form-state (r/atom {:tier nil :phone "" :status nil :message nil})
        upcoming-matches (filter #(#{:upcoming :live}
                                    (keyword (name (:match/status %))))
                                 (:matches @state))]
    (fn [state navigate!]
      (let [{:keys [tier phone status message]} @form-state]
        [:div.container
         [:div.section-header {:style {:margin-top "16px"}}
          [:h2.section-title (t :tickets/title)]]

         [:div.ticket-card.animate-slide-up
          ;; Select match
          (when (seq upcoming-matches)
            [:div.ticket-match-info
             [:h3 {:style {:font-family "var(--font-display)"
                           :font-size "1rem"
                           :margin-bottom "12px"}}
              "🏟️ " (t :home/upcoming)]
             (for [match (take 3 upcoming-matches)]
               (let [home (:match/home-club match)
                     away (:match/away-club match)]
                 ^{:key (:xt/id match)}
                 [:div.fixture-item {:style {:margin-bottom "8px"}
                                     :on-click #(navigate! :match-detail (:xt/id match))}
                  [:div.fixture-teams
                   [:span (or (:club/badge-emoji home) "🏟️")]
                   [:span (or (:club/short-name home) "")]
                   [:span.fixture-vs (str " " (t :match/vs) " ")]
                   [:span (or (:club/badge-emoji away) "🏟️")]
                   [:span (or (:club/short-name away) "")]]
                  [:div.fixture-meta
                   [:div (:match/venue match)]]]))])

          ;; Tier selection
          [:div {:style {:margin-bottom "24px"}}
           [:h3 {:style {:font-family "var(--font-display)"
                         :font-size "0.95rem"
                         :margin-bottom "12px"
                         :color "var(--text-secondary)"}}
            (t :tickets/select-tier)]
           [:div.tier-options
            [:div.tier-option {:class (when (= tier :vip) "selected")
                               :on-click #(swap! form-state assoc :tier :vip)}
             [:span.tier-name (str "🌟 " (t :tickets/vip))]
             [:span.tier-price "KES 2,000"]]
            [:div.tier-option {:class (when (= tier :main) "selected")
                               :on-click #(swap! form-state assoc :tier :main)}
             [:span.tier-name (str "🏟️ " (t :tickets/main))]
             [:span.tier-price "KES 500"]]
            [:div.tier-option {:class (when (= tier :terrace) "selected")
                               :on-click #(swap! form-state assoc :tier :terrace)}
             [:span.tier-name (str "⚽ " (t :tickets/terrace))]
             [:span.tier-price "KES 200"]]]]

          ;; Phone input
          [:input.mpesa-input {:type "tel"
                               :placeholder (t :tickets/phone)
                               :value phone
                               :max-length 12
                               :on-change #(swap! form-state assoc :phone (.. % -target -value))}]

          ;; Purchase button
          [:button.btn-mpesa
           {:disabled (or (nil? tier)
                          (< (count phone) 9)
                          (= status :processing))
            :on-click (fn []
                        (swap! form-state assoc :status :processing :message nil)
                        (if @offline/online?
                          ;; Simulate M-Pesa STK Push
                          (js/setTimeout
                           (fn []
                             (swap! form-state assoc
                                    :status :success
                                    :message (t :tickets/success)))
                           2000)
                          ;; Queue for later if offline
                          (do
                            (offline/queue-action! {:type :ticket-purchase
                                                    :tier tier
                                                    :phone phone})
                            (swap! form-state assoc
                                   :status :success
                                   :message "Tiketi imehifadhiwa. Itanunuliwa ukipata mtandao."))))}
           (if (= status :processing)
             [:<> [:div.spinner {:style {:width "20px" :height "20px"
                                          :border-width "2px"
                                          :margin-right "8px"}}]
              (t :tickets/processing)]
             [:<> "💳 " (t :tickets/buy)])]

          ;; Status message
          (when message
            [:div.payment-message {:class (name (or status :success))}
             message])]]))))
