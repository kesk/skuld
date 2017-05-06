(ns skuld.show-group.list-expenses-page
  (:require [skuld.show-group.state :as state :refer [app-state]]
            [reagent.core :as r]))

(defn list-expenses []
  (let [expenses @(r/track! state/expenses)]
    [:div
     [:h2 "Expenses"]
     [:table {:class "table"}
      [:thead>tr
       [:th "Who"]
       [:th "Amount"]
       [:th "When"]]
      [:tbody
       (for [{username :name, id :id, amount :amount, date :date} expenses]
         (let [date-obj (js/Date. date)
               date-str (.toLocaleDateString date-obj)
               time-str (.toLocaleTimeString date-obj)]
           ^{:key id}
           [:tr
            [:td username]
            [:td amount]
            [:td (str date-str " " time-str)]]))]]]))
