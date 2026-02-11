(ns harambee.sse
  "Server-Sent Events implementation for real-time match updates.
   Uses core.async channels with backpressure handling for 2G connections."
  (:require [clojure.core.async :as async :refer [go go-loop chan >! <! >!! put! close! mult tap untap]]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

;; ---------------------------------------------------------------------------
;; Event Bus — core.async pub/sub for match events
;; ---------------------------------------------------------------------------

(defonce ^:private event-channel (chan 1024))
(defonce ^:private event-mult (mult event-channel))

(defn publish-event!
  "Publish a match event to all connected SSE clients.
   Events are maps with :type, :match-id, and :data keys."
  [event]
  (log/debug "Publishing SSE event:" (:type event))
  (put! event-channel event))

(defn subscribe!
  "Create a new subscriber channel. Returns the channel.
   Caller must call unsubscribe! when done."
  []
  (let [ch (chan 64)]
    (tap event-mult ch)
    ch))

(defn unsubscribe!
  "Remove a subscriber channel."
  [ch]
  (untap event-mult ch)
  (close! ch))

;; ---------------------------------------------------------------------------
;; SSE Response Formatting
;; ---------------------------------------------------------------------------

(defn format-sse-event
  "Format an event map as an SSE text block."
  [{:keys [type data id]}]
  (str (when id (str "id: " id "\n"))
       "event: " (name type) "\n"
       "data: " (json/generate-string data) "\n\n"))

(defn sse-heartbeat-event
  "Create a heartbeat/ping event to keep connection alive on flaky networks."
  []
  (format-sse-event {:type :heartbeat
                     :data {:timestamp (System/currentTimeMillis)}}))

;; ---------------------------------------------------------------------------
;; SSE Ring Handler
;; ---------------------------------------------------------------------------

(defn match-events-handler
  "Ring async handler for SSE match events stream.
   Filters events for a specific match-id if provided."
  [request respond _raise]
  (let [match-id (get-in request [:path-params :id])
        sub-ch (subscribe!)]
    (log/info "SSE client connected for match:" (or match-id "all"))
    (respond
     {:status 200
      :headers {"Content-Type" "text/event-stream"
                "Cache-Control" "no-cache"
                "Connection" "keep-alive"
                "Access-Control-Allow-Origin" "*"
                "X-Accel-Buffering" "no"}
      :body (let [out (java.io.PipedOutputStream.)
                  in (java.io.PipedInputStream. out)]
              ;; Background goroutine to write events
              (go-loop []
                (if-let [event (<! sub-ch)]
                  (let [relevant? (or (nil? match-id)
                                      (= (:match-id event) match-id)
                                      (= (:type event) :heartbeat))]
                    (when relevant?
                      (try
                        (let [sse-text (format-sse-event event)]
                          (.write out (.getBytes sse-text "UTF-8"))
                          (.flush out))
                        (catch Exception e
                          (log/debug "SSE client disconnected:" (.getMessage e))
                          (unsubscribe! sub-ch)
                          (.close out))))
                    (recur))
                  (do
                    (log/debug "SSE subscription closed")
                    (.close out))))
              ;; Start heartbeat
              (go-loop []
                (async/<! (async/timeout 15000))
                (when (try
                        (.write out (.getBytes (sse-heartbeat-event) "UTF-8"))
                        (.flush out)
                        true
                        (catch Exception _ false))
                  (recur)))
              in)})))
