(ns harambee.payments
  "M-Pesa Daraja API integration for ticket purchases.
   SECURITY: Never stores PINs. All transactions are tokenized.
   Uses sandbox mode when credentials are not configured."
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import [java.util Base64]
           [java.time Instant ZoneId]
           [java.time.format DateTimeFormatter]))

;; ---------------------------------------------------------------------------
;; Configuration
;; ---------------------------------------------------------------------------

(def ^:private config
  {:consumer-key (System/getenv "MPESA_CONSUMER_KEY")
   :consumer-secret (System/getenv "MPESA_CONSUMER_SECRET")
   :shortcode (or (System/getenv "MPESA_SHORTCODE") "174379")
   :passkey (or (System/getenv "MPESA_PASSKEY") "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919")
   :callback-url (or (System/getenv "MPESA_CALLBACK_URL") "https://harambee-connect.ke/api/tickets/callback")
   :base-url (or (System/getenv "MPESA_BASE_URL") "https://sandbox.safaricom.co.ke")})

(defn sandbox-mode?
  "Check if running in sandbox/mock mode (no real credentials)."
  []
  (nil? (:consumer-key config)))

;; ---------------------------------------------------------------------------
;; Auth — OAuth token
;; ---------------------------------------------------------------------------

(defonce ^:private token-cache (atom nil))

(defn- get-auth-token
  "Get M-Pesa OAuth access token (cached)."
  []
  (if (sandbox-mode?)
    "SANDBOX_TOKEN"
    (let [cached @token-cache
          now (System/currentTimeMillis)]
      (if (and cached (< now (:expires-at cached)))
        (:token cached)
        (let [credentials (str (:consumer-key config) ":" (:consumer-secret config))
              encoded (.encodeToString (Base64/getEncoder) (.getBytes credentials))
              response (http/get (str (:base-url config) "/oauth/v1/generate?grant_type=client_credentials")
                                 {:headers {"Authorization" (str "Basic " encoded)}
                                  :as :json})
              token (get-in response [:body :access_token])
              expires-in (* 1000 (Integer/parseInt (get-in response [:body :expires_in] "3599")))]
          (reset! token-cache {:token token :expires-at (+ now expires-in)})
          token)))))

;; ---------------------------------------------------------------------------
;; Timestamp & Password
;; ---------------------------------------------------------------------------

(defn- mpesa-timestamp
  "Generate M-Pesa timestamp in YYYYMMDDHHmmss format (EAT timezone)."
  []
  (let [formatter (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")
        eat-zone (ZoneId/of "Africa/Nairobi")]
    (.format (.atZone (Instant/now) eat-zone) formatter)))

(defn- mpesa-password
  "Generate M-Pesa password: Base64(Shortcode + Passkey + Timestamp)."
  [timestamp]
  (let [raw (str (:shortcode config) (:passkey config) timestamp)]
    (.encodeToString (Base64/getEncoder) (.getBytes raw))))

;; ---------------------------------------------------------------------------
;; Ticket Pricing
;; ---------------------------------------------------------------------------

(def ticket-tiers
  {:vip {:name "VIP" :name-sw "VIP" :price 2000}
   :main {:name "Main Stand" :name-sw "Tribuni Kuu" :price 500}
   :terrace {:name "Terrace" :name-sw "Bao" :price 200}})

;; ---------------------------------------------------------------------------
;; STK Push — Initiate Payment
;; ---------------------------------------------------------------------------

(defn- validate-phone
  "Validate and normalize Kenyan phone number to 254XXXXXXXXX format.
   SECURITY: Strict validation, no dynamic code execution."
  [phone]
  (let [cleaned (str/replace (str phone) #"[^0-9]" "")]
    (cond
      (re-matches #"254[0-9]{9}" cleaned) cleaned
      (re-matches #"0[0-9]{9}" cleaned) (str "254" (subs cleaned 1))
      (re-matches #"7[0-9]{8}" cleaned) (str "254" cleaned)
      :else nil)))

(defn initiate-stk-push
  "Initiate M-Pesa STK Push for ticket purchase.
   Returns {:status :success/:error, :data ...}"
  [{:keys [phone match-id tier]}]
  (let [validated-phone (validate-phone phone)
        tier-info (get ticket-tiers (keyword tier))]
    (cond
      (nil? validated-phone)
      {:status :error :message "Invalid phone number. Use format 07XXXXXXXX"}

      (nil? tier-info)
      {:status :error :message "Invalid ticket tier"}

      (sandbox-mode?)
      (do
        (log/info "SANDBOX: STK Push to" validated-phone "for KES" (:price tier-info))
        {:status :success
         :sandbox true
         :data {:checkout-request-id (str "ws_CO_" (System/currentTimeMillis))
                :merchant-request-id (str "mr_" (System/currentTimeMillis))
                :phone validated-phone
                :amount (:price tier-info)
                :match-id match-id
                :tier tier
                :message "Sandbox mode: No real payment processed"}})

      :else
      (let [timestamp (mpesa-timestamp)
            password (mpesa-password timestamp)
            token (get-auth-token)
            payload {:BusinessShortCode (:shortcode config)
                     :Password password
                     :Timestamp timestamp
                     :TransactionType "CustomerPayBillOnline"
                     :Amount (:price tier-info)
                     :PartyA validated-phone
                     :PartyB (:shortcode config)
                     :PhoneNumber validated-phone
                     :CallBackURL (:callback-url config)
                     :AccountReference (str "TICKET-" match-id "-" tier)
                     :TransactionDesc (str "Match ticket: " tier " - " match-id)}]
        (try
          (let [response (http/post (str (:base-url config) "/mpesa/stkpush/v1/processrequest")
                                    {:headers {"Authorization" (str "Bearer " token)
                                               "Content-Type" "application/json"}
                                     :body (json/generate-string payload)
                                     :as :json})]
            (if (= "0" (get-in response [:body :ResponseCode]))
              {:status :success
               :data {:checkout-request-id (get-in response [:body :CheckoutRequestID])
                      :merchant-request-id (get-in response [:body :MerchantRequestID])
                      :phone validated-phone
                      :amount (:price tier-info)
                      :match-id match-id
                      :tier tier}}
              {:status :error
               :message (get-in response [:body :ResponseDescription] "Payment initiation failed")}))
          (catch Exception e
            (log/error e "M-Pesa STK Push failed")
            {:status :error :message "Payment service unavailable. Please try again."}))))))

;; ---------------------------------------------------------------------------
;; Callback Handler
;; ---------------------------------------------------------------------------

(defn handle-callback
  "Handle M-Pesa payment callback.
   SECURITY: Only processes expected fields, ignores unknown data."
  [callback-body]
  (let [result-code (get-in callback-body [:Body :stkCallback :ResultCode])
        checkout-id (get-in callback-body [:Body :stkCallback :CheckoutRequestID])]
    (log/info "M-Pesa callback received. ResultCode:" result-code "CheckoutID:" checkout-id)
    (if (zero? result-code)
      {:status :success
       :checkout-id checkout-id
       :message "Payment confirmed"}
      {:status :failed
       :checkout-id checkout-id
       :result-code result-code
       :message "Payment was not completed"})))
