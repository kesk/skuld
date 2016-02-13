(ns skuld.web-gui
  (:require [reagent.core :as r]))

(defn greeter [n]
  [:p "Hello " n "!"])

(r/render-component [greeter "Brah"]
                    (.-body js/document))
