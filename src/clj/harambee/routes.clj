(ns harambee.routes
  "HTTP API routes for Harambee Stars Connect.
   All routes use strict input validation — no eval, no dynamic resolution."
  (:require [reitit.ring :as ring]
            [ring.util.response :as resp]
            [harambee.db :as db]
            [harambee.match :as match]
            [harambee.club :as club]
            [harambee.payments :as payments]
            [harambee.sse :as sse]
            [harambee.middleware :as mw]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

;; ---------------------------------------------------------------------------
;; API Handlers
;; ---------------------------------------------------------------------------

(defn matches-handler
  "GET /api/matches — List all matches, optionally filtered by ?status=live|completed|upcoming"
  [request]
  (let [node (:db request)
        status (some-> (get-in request [:query-params "status"]) keyword)
        matches (if status
                  (match/get-enriched-matches node status)
                  (match/get-enriched-matches node))]
    {:status 200
     :body {:matches matches :count (count matches)}}))

(defn match-detail-handler
  "GET /api/matches/:id — Get match detail with events and lineups"
  [request]
  (let [node (:db request)
        match-id (get-in request [:path-params :id])
        m (match/get-match node match-id)]
    (if m
      {:status 200
       :body {:match (match/enrich-match node m)}}
      {:status 404
       :body {:error "Match not found"}})))

(defn clubs-handler
  "GET /api/clubs — List all clubs"
  [request]
  (let [node (:db request)
        clubs (club/get-all-clubs node)]
    {:status 200
     :body {:clubs clubs :count (count clubs)}}))

(defn club-detail-handler
  "GET /api/clubs/:id — Get club detail with fixtures"
  [request]
  (let [node (:db request)
        club-id (get-in request [:path-params :id])
        c (club/get-club-with-fixtures node club-id)]
    (if c
      {:status 200
       :body {:club c}}
      {:status 404
       :body {:error "Club not found"}})))

(defn standings-handler
  "GET /api/standings — Get league standings"
  [request]
  (let [node (:db request)
        standings (match/compute-standings node)]
    {:status 200
     :body {:standings standings}}))

(defn ticket-purchase-handler
  "POST /api/tickets/purchase — Initiate M-Pesa ticket purchase"
  [request]
  (let [body (:body request)
        phone (:phone body)
        match-id (:match-id body)
        tier (:tier body)]
    (if (and phone match-id tier)
      (let [result (payments/initiate-stk-push {:phone phone
                                                :match-id match-id
                                                :tier tier})]
        {:status (if (= :success (:status result)) 200 400)
         :body result})
      {:status 400
       :body {:error "Missing required fields: phone, match-id, tier"}})))

(defn ticket-callback-handler
  "POST /api/tickets/callback — M-Pesa payment callback"
  [request]
  (let [body (:body request)
        result (payments/handle-callback body)]
    {:status 200
     :body result}))

(defn ticket-tiers-handler
  "GET /api/tickets/tiers — Get available ticket tiers and prices"
  [_request]
  {:status 200
   :body {:tiers payments/ticket-tiers}})

(defn health-handler
  "GET /api/health — Health check endpoint"
  [_request]
  {:status 200
   :body {:status "ok"
          :service "harambee-stars-connect"
          :version "0.1.0"
          :sandbox (payments/sandbox-mode?)}})

;; ---------------------------------------------------------------------------
;; SPA Fallback Handler
;; ---------------------------------------------------------------------------

(defn spa-handler
  "Serve the SPA index.html for any non-API, non-static route."
  [_request]
  (if-let [resource (io/resource "public/index.html")]
    (-> (resp/response (slurp resource))
        (resp/content-type "text/html; charset=utf-8"))
    {:status 404
     :body "Not found"}))

;; ---------------------------------------------------------------------------
;; Router
;; ---------------------------------------------------------------------------

(defn inject-db
  "Middleware to inject XTDB node into request."
  [node]
  (fn [handler]
    (fn [request]
      (handler (assoc request :db node)))))

(defn create-router
  "Create the Reitit ring router with all routes."
  [xtdb-node]
  (ring/ring-handler
   (ring/router
    [;; API routes
     ["/api"
      ["/health" {:get health-handler}]
      ["/matches" {:get matches-handler}]
      ["/matches/:id" {:get match-detail-handler}]
      ["/matches/:id/events" {:get {:handler sse/match-events-handler}}]
      ["/clubs" {:get clubs-handler}]
      ["/clubs/:id" {:get club-detail-handler}]
      ["/standings" {:get standings-handler}]
      ["/tickets"
       ["/tiers" {:get ticket-tiers-handler}]
       ["/purchase" {:post ticket-purchase-handler}]
       ["/callback" {:post ticket-callback-handler}]]]]

    ;; Router config
    {:data {:middleware [mw/wrap-cors
                         mw/wrap-security-headers
                         mw/wrap-request-logging
                         mw/wrap-json-body
                         mw/wrap-json-response
                         mw/wrap-input-sanitization
                         (mw/wrap-rate-limit)
                         (inject-db xtdb-node)]}})

   ;; Default handler: serve static files or SPA
   (ring/routes
    (ring/create-resource-handler {:path "/"
                                   :root "public"})
    (ring/create-default-handler
     {:not-found spa-handler}))))
