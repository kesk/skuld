(ns skuld.show-group
  (:require [skuld.common :refer [get-element]]
            [reagent.core :as r]
            [ajax.core :refer [GET]]))

(defonce group-info (r/atom nil))

(defn show-group [group-id]
  [:div
   [:p (str "Nice group! " group-id)]
   [:pre (str @group-info)]])

(defn init [match]
  (GET (str "/api/v1/groups/" (match 1))
       {:handler #(reset! group-info %)
        :error #(.log js/console %)
        :response-format :json
        :keywords? true})
  (r/render-component [show-group (match 1)] (get-element "show-group")))
