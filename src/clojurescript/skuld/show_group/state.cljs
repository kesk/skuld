(ns skuld.show-group.state
  (:require [reagent.core :as r]))

(defonce app-state (r/atom {:expense-form {:payed-by ""
                                           :amount ""
                                           :shared-with {}}}))

(defn- event-handler [state [event-name value]]
  (case event-name
    :group-info (-> state
                    (update :group-info merge (update value :members (partial sort :name)))
                    (assoc-in [:group-info :member-id-map]
                              (reduce (fn [acc v] (assoc acc (:id v) v)) {} (:members value)))
                    (assoc-in [:expense-form :shared-with]
                              (into {} (map vector (map :id (:members value)) (repeat true))))
                    (assoc-in [:expense-form :payed-by] (-> value :members first :id)))
    :group-expenses (assoc state :group-expenses value)
    :shared-with (update-in state [:expense-form :shared-with value] not)
    :change-amount (assoc-in state [:expense-form :amount] value)
    :change-payed-by (assoc-in state [:expense-form :payed-by] (js/parseInt value))
    :change-page (assoc state :page value)
    :change-group-id (assoc-in state [:group-info :id] value)
    state))

(defn dispatch! [e]
  (r/rswap! app-state event-handler e))

(defn expenses []
  (:group-expenses @app-state))
