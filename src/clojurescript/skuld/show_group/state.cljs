(ns skuld.show-group.state
  (:require [reagent.core :as r]))

(defonce app-state (r/atom {:page :home
                            :expense-form {:payed-by ""
                                           :amount ""
                                           :shared-with {}}}))

(defn- event-handler [state [event-name value]]
  (case event-name
    :group-info (-> state
                    (update :group-info merge (update value :members sort))
                    (assoc-in [:expense-form :shared-with]
                              (into {} (map #(vec [% true]) (:members value))))
                    (assoc-in [:expense-form :payed-by] (first (:members value))))
    :group-expenses (assoc state :group-expenses value)
    :shared-with (update-in state [:expense-form :shared-with value] not)
    :change-amount (assoc-in state [:expense-form :amount] value)
    :change-payed-by (assoc-in state [:expense-form :payed-by] value)
    state))

(defn dispatch! [e]
  (r/rswap! app-state event-handler e))

