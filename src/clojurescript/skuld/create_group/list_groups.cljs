(ns skuld.create-group.list-groups
  (:require [skuld.common :refer [get-element]]
            [reagent.core :as r]
            [ajax.core :refer [GET]]))

(defonce groups (r/atom []))

(defn group-list []
  [:ul
   (for [group @groups] ^{:key (:id group)}
     [:li [:a {:href (str "/groups/" (:id group))}
           (:name group)]])])

(defn- get-group-list []
  (GET "/api/v1/groups"
       {:handler #(reset! groups %)
        :error #(.log js/console %)
        :response-format :json
        :keywords? true}))

(defn init [element-id]
  (get-group-list)
  (r/render [group-list] (get-element element-id)))
