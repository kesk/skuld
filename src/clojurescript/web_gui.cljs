(ns skuld.web-gui
  (:require [reagent.core :as r]
            [clojure.string :as s]))

(defonce value (r/atom "foo"))

(defn get-element [id]
  (.getElementById js/document id))

(defn greeter [n]
  [:p "Hello " n "!"])

(defn atom-input [value]
  [:input {:type "text"
           :value @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn shared-state []
  [:div
   [:p "The value is now: " (s/upper-case @value)]
   [:p "Change it here: " [atom-input value]]])

(defn init-start []
  (r/render-component [shared-state] (get-element "app")))

(let [pathname (-> js/window .-location .-pathname)]
  (condp re-matches pathname
    #"/" (init-start)))

