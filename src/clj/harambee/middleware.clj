(ns harambee.middleware
  "Security-focused middleware stack.
   INVARIANTS:
   - No eval, no dynamic code execution
   - Strict input validation on all endpoints
   - CORS configured for PWA origin
   - Rate limiting to prevent abuse"
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

;; ---------------------------------------------------------------------------
;; CORS Middleware
;; ---------------------------------------------------------------------------

(defn wrap-cors
  "Add CORS headers for cross-origin PWA requests."
  [handler]
  (fn [request]
    (if (= :options (:request-method request))
      {:status 204
       :headers {"Access-Control-Allow-Origin" "*"
                 "Access-Control-Allow-Methods" "GET, POST, OPTIONS"
                 "Access-Control-Allow-Headers" "Content-Type, Authorization"
                 "Access-Control-Max-Age" "86400"}}
      (let [response (handler request)]
        (update response :headers merge
                {"Access-Control-Allow-Origin" "*"
                 "Access-Control-Allow-Methods" "GET, POST, OPTIONS"
                 "Access-Control-Allow-Headers" "Content-Type, Authorization"})))))

;; ---------------------------------------------------------------------------
;; Content-Type Middleware
;; ---------------------------------------------------------------------------

(defn wrap-json-response
  "Ensure API responses have proper JSON content-type."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (and (map? (:body response))
               (not (get-in response [:headers "Content-Type"])))
        (-> response
            (update :headers assoc "Content-Type" "application/json; charset=utf-8")
            (update :body json/generate-string))
        response))))

(defn wrap-json-body
  "Parse JSON request bodies."
  [handler]
  (fn [request]
    (if (and (some-> (get-in request [:headers "content-type"])
                     (str/includes? "application/json"))
             (:body request))
      (try
        (let [body (json/parse-string (slurp (:body request)) true)]
          (handler (assoc request :body body)))
        (catch Exception _e
          {:status 400
           :headers {"Content-Type" "application/json"}
           :body (json/generate-string {:error "Invalid JSON in request body"})}))
      (handler request))))

;; ---------------------------------------------------------------------------
;; Security Middleware
;; ---------------------------------------------------------------------------

(defn wrap-security-headers
  "Add security headers to all responses."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (update response :headers merge
              {"X-Content-Type-Options" "nosniff"
               "X-Frame-Options" "DENY"
               "X-XSS-Protection" "1; mode=block"
               "Referrer-Policy" "strict-origin-when-cross-origin"
               "Content-Security-Policy" "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'"}))))

(defn wrap-input-sanitization
  "Sanitize inputs to prevent injection attacks.
   SECURITY: Strip any potentially dangerous characters from string inputs."
  [handler]
  (fn [request]
    (let [sanitize-str (fn [s]
                         (when (string? s)
                           (-> s
                               (str/replace #"<script[^>]*>.*?</script>" "")
                               (str/replace #"<[^>]+>" "")
                               (str/trim))))
          sanitize-map (fn [m]
                         (when (map? m)
                           (reduce-kv
                            (fn [acc k v]
                              (assoc acc k (if (string? v) (sanitize-str v) v)))
                            {}
                            m)))
          sanitized-request (cond-> request
                              (:params request)
                              (update :params sanitize-map)
                              (:body request)
                              (update :body (fn [body]
                                              (if (map? body)
                                                (sanitize-map body)
                                                body))))]
      (handler sanitized-request))))

;; ---------------------------------------------------------------------------
;; Rate Limiting (Simple in-memory)
;; ---------------------------------------------------------------------------

(defonce ^:private rate-limit-store (atom {}))

(defn wrap-rate-limit
  "Simple in-memory rate limiting. Limits per IP per minute.
   Returns a Ring middleware function."
  ([] (wrap-rate-limit 60 60000))
  ([max-requests window-ms]
   (fn [handler]
     (fn [request]
       (let [client-ip (or (get-in request [:headers "x-forwarded-for"])
                           (:remote-addr request)
                           "unknown")
             now (System/currentTimeMillis)
             window-start (- now window-ms)]
         ;; Clean old entries
         (swap! rate-limit-store
                (fn [store]
                  (reduce-kv
                   (fn [acc k v]
                     (let [recent (filter #(> % window-start) v)]
                       (if (seq recent) (assoc acc k recent) acc)))
                   {}
                   store)))
         ;; Check rate limit
         (let [requests (get @rate-limit-store client-ip [])
               recent-count (count (filter #(> % window-start) requests))]
           (if (>= recent-count max-requests)
             {:status 429
              :headers {"Content-Type" "application/json"
                        "Retry-After" "60"}
              :body (json/generate-string {:error "Too many requests. Please try again later."
                                           :retry-after 60})}
             (do
               (swap! rate-limit-store update client-ip (fnil conj []) now)
               (handler request)))))))))

;; ---------------------------------------------------------------------------
;; Request Logging
;; ---------------------------------------------------------------------------

(defn wrap-request-logging
  "Log incoming requests for debugging."
  [handler]
  (fn [request]
    (let [start (System/currentTimeMillis)
          response (handler request)
          duration (- (System/currentTimeMillis) start)]
      (log/info (format "%s %s %d (%dms)"
                        (str/upper-case (name (:request-method request)))
                        (:uri request)
                        (:status response 500)
                        duration))
      response)))

;; ---------------------------------------------------------------------------
;; Static Files Middleware
;; ---------------------------------------------------------------------------

(defn wrap-static-files
  "Serve static files from resources/public with proper caching."
  [handler]
  (fn [request]
    (let [uri (:uri request)
          response (handler request)]
      (if (and response
               (or (str/ends-with? uri ".css")
                   (str/ends-with? uri ".js")
                   (str/ends-with? uri ".json")))
        (update response :headers assoc
                "Cache-Control" "public, max-age=3600")
        response))))
