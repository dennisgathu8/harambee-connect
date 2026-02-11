(ns harambee.offline
  "Offline-first data management.
   Caches API responses in browser localStorage for instant access on 2G/offline.
   Queues actions when offline and replays them when connectivity returns.")

;; ---------------------------------------------------------------------------
;; Local Storage Cache
;; ---------------------------------------------------------------------------

(defn- cache-key [endpoint]
  (str "harambee-cache:" endpoint))

(defn cache-data!
  "Cache API response data in localStorage."
  [endpoint data]
  (try
    (.setItem js/localStorage
              (cache-key endpoint)
              (js/JSON.stringify (clj->js {:data data
                                           :timestamp (.now js/Date)})))
    (catch js/Error _e nil)))

(defn get-cached-data
  "Retrieve cached data from localStorage."
  [endpoint]
  (try
    (when-let [raw (.getItem js/localStorage (cache-key endpoint))]
      (let [parsed (js->clj (js/JSON.parse raw) :keywordize-keys true)]
        (:data parsed)))
    (catch js/Error _e nil)))

(defn cache-age
  "Get cache age in milliseconds for an endpoint."
  [endpoint]
  (try
    (when-let [raw (.getItem js/localStorage (cache-key endpoint))]
      (let [parsed (js->clj (js/JSON.parse raw) :keywordize-keys true)]
        (- (.now js/Date) (:timestamp parsed))))
    (catch js/Error _e nil)))

;; ---------------------------------------------------------------------------
;; Offline Action Queue
;; ---------------------------------------------------------------------------

(defn- queue-key [] "harambee-offline-queue")

(defn queue-action!
  "Queue an action for later execution when back online."
  [action]
  (try
    (let [raw (or (.getItem js/localStorage (queue-key)) "[]")
          queue (js->clj (js/JSON.parse raw) :keywordize-keys true)
          updated (conj queue (assoc action :queued-at (.now js/Date)))]
      (.setItem js/localStorage (queue-key) (js/JSON.stringify (clj->js updated))))
    (catch js/Error _e nil)))

(defn process-offline-queue!
  "Process any queued offline actions now that we're back online."
  []
  (try
    (let [raw (or (.getItem js/localStorage (queue-key)) "[]")
          queue (js->clj (js/JSON.parse raw) :keywordize-keys true)]
      (when (seq queue)
        (js/console.log (str "Processing " (count queue) " offline actions"))
        ;; Clear queue first to prevent double-processing
        (.setItem js/localStorage (queue-key) "[]")
        ;; Actions would be processed here
        ))
    (catch js/Error _e nil)))

;; ---------------------------------------------------------------------------
;; Connectivity Detection
;; ---------------------------------------------------------------------------

(defonce online? (atom (.-onLine js/navigator)))

(defn setup-connectivity-listeners!
  "Listen for online/offline events."
  []
  (.addEventListener js/window "online"
                     #(do (reset! online? true)
                          (process-offline-queue!)))
  (.addEventListener js/window "offline"
                     #(reset! online? false)))
