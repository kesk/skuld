(ns skuld.web-gui
  (:require [reagent.core :as r]))

(defonce value (r/atom "foo"))

(defn greeter [n]
  [:p "Hello " n "!"])

(defn atom-input [value]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn shared-state []
  [:div
   [:p "The value is now: " @value]
   [:p "Change it here: " [atom-input value]]])

(r/render-component [shared-state]
                    (.getElementById js/document "app"))
