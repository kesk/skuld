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

(secretary/set-config! :prefix "#")

(defroute "/:id" [id]
  (dispatch! [:change-page :home])
  (dispatch! [:change-group-id id]))

(defroute "/:id/expenses" [id]
  (dispatch! [:change-page :list-expenses])
  (dispatch! [:change-group-id id]))

(defroute "*" []
  (dispatch! [:change-page :not-found]))

(defonce hook-browser-navigation!
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn- group-id []
  (get-in @app-state [:group-info :id]))

(defn- get-group-info []
  (if-let [group-id @(r/track! group-id)]
    (GET (str "/api/v1/groups/" group-id)
         {:handler #(dispatch! [:group-info %])
          :error #(.log js/console %)
          :response-format :json
          :keywords? true})))

(defn- get-group-expenses []
  (if-let [group-id @(r/track! group-id)]
    (GET (str "/api/v1/groups/" group-id "/expenses")
         {:handler #(dispatch! [:group-expenses %])
          :error #(.log js/console %)
          :response-format :json
          :keywords? true})))

(defonce group-info-updater
  (do
    (r/track! get-group-info)
    (r/track! get-group-expenses)))

(defn list-expenses []
  [:div>h2 "Expenses"])

(defmulti current-page #(@app-state :page))
(defmethod current-page :home []
  [home])
(defmethod current-page :list-expenses []
  [list-expenses])
(defmethod current-page :not-found []
  [:p "Not found"])
(defmethod current-page :default []
  [:p "Loading"])

(defn- group-name []
  (-> @app-state :group-info :name))

(defn- base-page [content]
  (let [group-name @(r/track group-name)]
    [:div
     [:h1 {:class "group-header"} group-name]
     [content]]))

(defn ^:export init []
  (r/render [base-page current-page] (get-element "app")))

