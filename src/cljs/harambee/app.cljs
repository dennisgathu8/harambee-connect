(ns harambee.app
  "Main application entry point for Harambee Stars Connect PWA.
   Manages routing, state, API communication, and SSE connections."
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [harambee.i18n :as i18n]
            [harambee.offline :as offline]
            [harambee.components :as c]
            [harambee.views.home :as home]
            [harambee.views.match :as match]
            [harambee.views.clubs :as clubs]
            [harambee.views.standings :as standings]
            [harambee.views.tickets :as tickets]))

;; ---------------------------------------------------------------------------
;; Application State
;; ---------------------------------------------------------------------------

(defonce app-state
  (r/atom {:page :home
           :matches []
           :clubs []
           :standings []
           :selected-match nil
           :selected-club nil
           :loading? true
           :error nil}))

;; ---------------------------------------------------------------------------
;; API Communication
;; ---------------------------------------------------------------------------

(defn fetch-json
  "Fetch JSON from API with offline fallback."
  [endpoint callback]
  (let [url (str "/api" endpoint)]
    (-> (js/fetch url)
        (.then (fn [response]
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP " (.-status response)))))))
        (.then (fn [data]
                 (let [clj-data (js->clj data :keywordize-keys true)]
                   ;; Cache for offline
                   (offline/cache-data! endpoint clj-data)
                   (callback clj-data))))
        (.catch (fn [err]
                  (js/console.warn "API fetch failed, trying cache:" (.-message err))
                  ;; Fallback to cached data
                  (when-let [cached (offline/get-cached-data endpoint)]
                    (callback cached)))))))

(defn load-data!
  "Load initial data from API."
  []
  (fetch-json "/matches"
              (fn [data]
                (swap! app-state assoc :matches (or (:matches data) []) :loading? false)))
  (fetch-json "/clubs"
              (fn [data]
                (swap! app-state assoc :clubs (or (:clubs data) []))))
  (fetch-json "/standings"
              (fn [data]
                (swap! app-state assoc :standings (or (:standings data) [])))))

;; ---------------------------------------------------------------------------
;; SSE (Server-Sent Events) Connection
;; ---------------------------------------------------------------------------

(defonce sse-connection (atom nil))

(defn connect-sse!
  "Connect to SSE for live match updates."
  []
  (when-let [old @sse-connection]
    (.close old))
  (try
    (let [es (js/EventSource. "/api/matches/all/events")]
      (set! (.-onmessage es)
            (fn [event]
              (let [data (js->clj (js/JSON.parse (.-data event)) :keywordize-keys true)]
                (js/console.log "SSE event:" data)
                ;; Reload match data on events
                (fetch-json "/matches"
                            (fn [d]
                              (swap! app-state assoc :matches (or (:matches d) [])))))))
      (set! (.-onerror es)
            (fn [_]
              (js/console.warn "SSE connection error, will retry...")))
      (reset! sse-connection es))
    (catch js/Error e
      (js/console.warn "SSE not available:" (.-message e)))))

;; ---------------------------------------------------------------------------
;; Navigation
;; ---------------------------------------------------------------------------

(defn navigate!
  "Navigate to a page, optionally with a detail ID."
  ([page]
   (swap! app-state assoc :page page))
  ([page detail-id]
   (case page
     :match-detail (swap! app-state assoc :page :match-detail :selected-match detail-id)
     :club-detail (swap! app-state assoc :page :club-detail :selected-club detail-id)
     (swap! app-state assoc :page page))))

;; ---------------------------------------------------------------------------
;; Root Component
;; ---------------------------------------------------------------------------

(defn current-page []
  (let [page (:page @app-state)]
    [:div
     [c/header]
     [:main.main-content
      (case page
        :home [home/home-page app-state navigate!]
        :matches [match/matches-page app-state navigate!]
        :match-detail [match/match-detail-page app-state navigate!]
        :clubs [clubs/clubs-page app-state navigate!]
        :club-detail [clubs/club-detail-page app-state navigate!]
        :standings [standings/standings-page app-state navigate!]
        :tickets [tickets/tickets-page app-state navigate!]
        [home/home-page app-state navigate!])]
     [c/navbar (:page @app-state) navigate!]]))

;; ---------------------------------------------------------------------------
;; Mount & Init
;; ---------------------------------------------------------------------------

(defn mount-root! []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (js/console.log "🇰🇪 Harambee Stars Connect — Karibu!")
  (offline/setup-connectivity-listeners!)
  (load-data!)
  (connect-sse!)
  (mount-root!))
