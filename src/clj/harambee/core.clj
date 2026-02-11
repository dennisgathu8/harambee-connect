(ns harambee.core
  "Application entry point for Harambee Stars Connect.
   Starts the HTTP server and seeds the database."
  (:require [ring.adapter.jetty :as jetty]
            [mount.core :as mount :refer [defstate]]
            [harambee.db :as db]
            [harambee.routes :as routes]
            [clojure.tools.logging :as log])
  (:gen-class))

;; ---------------------------------------------------------------------------
;; HTTP Server
;; ---------------------------------------------------------------------------

(defstate http-server
  :start (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
               handler (routes/create-router db/xtdb-node)]
           ;; Seed database
           (db/seed-data! db/xtdb-node)
           (println "")
           (println "=== 🇰🇪 Harambee Stars Connect ===")
           (println (str "Server starting on port " port))
           (println (str "→ http://localhost:" port))
           (println (str "→ API: http://localhost:" port "/api/health"))
           (println (str "→ M-Pesa: " (if (harambee.payments/sandbox-mode?) "SANDBOX" "PRODUCTION")))
           (println "================================")
           (println "")
           (jetty/run-jetty handler {:port port :join? false}))
  :stop (do
          (println "Shutting down server...")
          (.stop http-server)))

;; ---------------------------------------------------------------------------
;; Main Entry Point
;; ---------------------------------------------------------------------------

(defn -main
  "Start the application."
  [& _args]
  (mount/start)
  (log/info "Harambee Stars Connect is running! Karibu! 🇰🇪"))
