(defproject harambee-stars-connect "0.1.0-SNAPSHOT"
  :description "Harambee Stars Connect — The Kenyan Premier League Digital Platform"
  :url "https://github.com/dennisgathu8/harambee-stars-connect"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; Web server
                 [ring/ring-core "1.10.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 [ring/ring-json "0.5.1"]
                 ;; Routing
                 [metosin/reitit "0.7.0-alpha7"]
                 [metosin/reitit-ring "0.7.0-alpha7"]
                 ;; Database — XTDB (immutable, bitemporal)
                 [com.xtdb/xtdb-core "1.24.1"]
                 ;; JSON
                 [cheshire "5.12.0"]
                 ;; HTTP client (for M-Pesa API)
                 [clj-http "3.12.3"]
                 ;; Lifecycle management
                 [mount "0.1.17"]
                 ;; Async
                 [org.clojure/core.async "1.6.681"]
                 ;; Logging
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.2.12"]
                 ;; Time
                 [clojure.java-time/clojure.java-time "1.3.0"]
                 ;; Security — input validation
                 [metosin/malli "0.13.0"]]

  :source-paths ["src/clj"]
  :resource-paths ["resources"]

  :main harambee.core

  :profiles {:dev {:dependencies [[ring/ring-mock "0.4.0"]]
                   :resource-paths ["dev-resources"]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
