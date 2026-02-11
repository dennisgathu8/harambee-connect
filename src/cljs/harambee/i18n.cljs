(ns harambee.i18n
  "Bilingual support: Swahili (default) and English.
   Translations stored as EDN maps, no heavy i18n library needed."
  (:require [reagent.core :as r]))

;; ---------------------------------------------------------------------------
;; State
;; ---------------------------------------------------------------------------

(defonce current-lang (r/atom :sw))

;; ---------------------------------------------------------------------------
;; Translation Dictionary (embedded for offline support)
;; ---------------------------------------------------------------------------

(def translations
  {:sw
   {;; Navigation
    :nav/home "Nyumbani"
    :nav/matches "Mechi"
    :nav/clubs "Klabu"
    :nav/standings "Msimamo"
    :nav/tickets "Tiketi"
    :nav/live "🔴 MOJA KWA MOJA"

    ;; Home
    :home/title "Harambee Stars Connect"
    :home/subtitle "Jukwaa la Soka la Kenya"
    :home/live-matches "Mechi za Moja kwa Moja"
    :home/upcoming "Mechi Zijazo"
    :home/results "Matokeo"
    :home/no-live "Hakuna mechi za moja kwa moja kwa sasa"
    :home/hero-text "Kila mashabiki ameunganishwa. Kila mechi moja kwa moja. Kila wakati hauwezi kubadilishwa."

    ;; Match
    :match/live "MOJA KWA MOJA"
    :match/completed "IMEKWISHA"
    :match/upcoming "ITAKUJA"
    :match/minute "Dakika"
    :match/goal "Goli"
    :match/yellow-card "Kadi ya Njano"
    :match/red-card "Kadi ya Nyekundu"
    :match/substitution "Ubadilishaji"
    :match/lineup "Kikosi"
    :match/events "Matukio"
    :match/venue "Uwanja"
    :match/referee "Refa"
    :match/attendance "Mahudhurio"
    :match/vs "dhidi ya"

    ;; Club
    :club/squad "Kikosi"
    :club/fixtures "Ratiba"
    :club/founded "Iliyoanzishwa"
    :club/stadium "Uwanja"
    :club/city "Jiji"
    :club/nickname "Jina la Utani"

    ;; Standings
    :standings/title "Msimamo wa Ligi"
    :standings/position "Naf."
    :standings/team "Timu"
    :standings/played "M"
    :standings/won "U"
    :standings/drawn "S"
    :standings/lost "H"
    :standings/goals-for "GM"
    :standings/goals-against "GD"
    :standings/goal-diff "TG"
    :standings/points "Pt"

    ;; Tickets
    :tickets/title "Nunua Tiketi"
    :tickets/select-tier "Chagua Daraja"
    :tickets/vip "VIP"
    :tickets/main "Tribuni Kuu"
    :tickets/terrace "Bao"
    :tickets/phone "Nambari ya Simu (M-Pesa)"
    :tickets/buy "Nunua kwa M-Pesa"
    :tickets/processing "Inaprocess..."
    :tickets/success "Tiketi umenunuliwa! Angalia M-Pesa yako."
    :tickets/error "Kuna tatizo. Jaribu tena."

    ;; Offline
    :offline/indicator "📡 Nje ya Mtandao"
    :offline/cached "Data iliyohifadhiwa"

    ;; Common
    :common/loading "Inapakia..."
    :common/error "Hitilafu imetokea"
    :common/retry "Jaribu Tena"
    :common/back "Rudi"
    :common/language "English"
    :common/see-all "Ona Zote"}

   :en
   {:nav/home "Home"
    :nav/matches "Matches"
    :nav/clubs "Clubs"
    :nav/standings "Standings"
    :nav/tickets "Tickets"
    :nav/live "🔴 LIVE"

    :home/title "Harambee Stars Connect"
    :home/subtitle "Kenya's Football Platform"
    :home/live-matches "Live Matches"
    :home/upcoming "Upcoming Matches"
    :home/results "Results"
    :home/no-live "No live matches right now"
    :home/hero-text "Every fan connected. Every match live. Every moment immutable."

    :match/live "LIVE"
    :match/completed "FULL TIME"
    :match/upcoming "UPCOMING"
    :match/minute "Minute"
    :match/goal "Goal"
    :match/yellow-card "Yellow Card"
    :match/red-card "Red Card"
    :match/substitution "Substitution"
    :match/lineup "Lineup"
    :match/events "Events"
    :match/venue "Venue"
    :match/referee "Referee"
    :match/attendance "Attendance"
    :match/vs "vs"

    :club/squad "Squad"
    :club/fixtures "Fixtures"
    :club/founded "Founded"
    :club/stadium "Stadium"
    :club/city "City"
    :club/nickname "Nickname"

    :standings/title "League Standings"
    :standings/position "Pos"
    :standings/team "Team"
    :standings/played "P"
    :standings/won "W"
    :standings/drawn "D"
    :standings/lost "L"
    :standings/goals-for "GF"
    :standings/goals-against "GA"
    :standings/goal-diff "GD"
    :standings/points "Pts"

    :tickets/title "Buy Tickets"
    :tickets/select-tier "Select Tier"
    :tickets/vip "VIP"
    :tickets/main "Main Stand"
    :tickets/terrace "Terrace"
    :tickets/phone "Phone Number (M-Pesa)"
    :tickets/buy "Pay with M-Pesa"
    :tickets/processing "Processing..."
    :tickets/success "Ticket purchased! Check your M-Pesa."
    :tickets/error "Something went wrong. Please try again."

    :offline/indicator "📡 Offline"
    :offline/cached "Cached data"

    :common/loading "Loading..."
    :common/error "An error occurred"
    :common/retry "Retry"
    :common/back "Back"
    :common/language "Kiswahili"
    :common/see-all "See All"}})

;; ---------------------------------------------------------------------------
;; Translation Function
;; ---------------------------------------------------------------------------

(defn t
  "Translate a keyword to the current language."
  [k]
  (or (get-in translations [@current-lang k])
      (get-in translations [:en k])
      (name k)))

(defn toggle-lang!
  "Toggle between Swahili and English."
  []
  (swap! current-lang #(if (= % :sw) :en :sw)))
