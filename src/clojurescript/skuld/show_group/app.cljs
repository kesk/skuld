(ns skuld.show-group.app
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [skuld.common :refer [get-element]]
            [skuld.show-group.state :refer [app-state dispatch!]]
            [skuld.show-group.add-expense-page :refer [home]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [reagent.core :as r]
            [ajax.core :refer [GET POST]]))

(defonce hook-browser-navigation!
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defonce app-routes
  (do
    (secretary/set-config! :prefix "#")

    (defroute "/" []
      (swap! app-state assoc :page :home))

    (defroute "/expenses" []
      (swap! app-state assoc :page :list-expenses))

    (defroute "*" []
      (swap! app-state assoc :page :not-found))))

(defn list-expenses []
  [:div>h1 "Expenses"])

(def group-id
  (->> js/window
       .-location
       .-pathname
       (re-find #"/groups/([\d\w-]*)")
       (#(% 1))))

(defmulti current-page #(@app-state :page))
(defmethod current-page :home []
  [home])
(defmethod current-page :list-expenses []
  [list-expenses])
(defmethod current-page :not-found []
  [:p "Page not found"])

(defn get-group-info []
  (GET (str "/api/v1/groups/" group-id)
       {:handler #(dispatch! [:group-info %])
        :error #(.log js/console %)
        :response-format :json
        :keywords? true}))

(defn get-group-expenses []
  (GET (str "/api/v1/groups/" group-id "/expenses")
       {:handler #(dispatch! [:group-expenses %])
        :error #(.log js/console %)
        :response-format :json
        :keywords? true}))

(defn- group-name []
  (-> @app-state :group-info :name))

(defn- base-page [content]
  (let [group-name @(r/track group-name)]
    [:div
     [:h1 group-name]
     [content]]))

(defn ^:export init []
  (r/render [base-page current-page] (get-element "app"))
  (get-group-info)
  (get-group-expenses))

