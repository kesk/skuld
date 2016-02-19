(ns skuld.web-gui
  (:require [reagent.core :as r]
            [clojure.string :as s]))

(defonce value (r/atom "foo"))

(defn get-element [id]
  (.getElementById js/document id))

(defonce group-members (r/atom ["John" "Liza"]))

(defn print-group-members [members]
  [:pre (s/join "\n" (map-indexed #(str %1 ": " %2) members))])

(defn group-member-form [members]
  (into [:div]
        (map-indexed
          (fn [idx member-name]
            [:div
             [:input {:type "text"
                      :value member-name
                      :on-change #(swap! members assoc idx (-> % .-target .-value))}]])
          @members)))

(defn create-group-form []
  [:div {:class "container"}
   [:form
    [:p "Group name:"]
    [:input {:type "text"}]
    [:p "Input names of group members:"]
    [group-member-form group-members]]
   [:button {:type "button"
             :on-click #(swap! group-members conj "")}
    "+1 group member"]
   [:button {:type "button"
             :on-click #(swap! group-members (comp vec drop-last))}
    "-1 group member"]
   [print-group-members @group-members]])

(defn init-start []
  (r/render-component [create-group-form] (get-element "app")))

; Some routing (sort of)
(condp re-matches (-> js/window .-location .-pathname)
  #"/" (init-start))

