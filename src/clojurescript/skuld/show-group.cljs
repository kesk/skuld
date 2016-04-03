(ns skuld.show-group
  (:require [skuld.common :refer [get-element]]
            [reagent.core :as r]
            [ajax.core :refer [GET]]))

(defonce group-info (r/atom {}))

(def group-id
  (->> js/window
       .-location
       .-pathname
       (re-find #"/groups/([\d\w-]*)")
       (#(% 1))))

(defn get-group-info []
  (GET (str "/api/v1/groups/" group-id)
       {:handler #(reset! group-info %)
        :error #(.log js/console %)
        :response-format :json
        :keywords? true}))

(defn show-group []
  [:div
   [:p (str "Group id: " (:id @group-info))]
   [:h1 (:name @group-info)]
   [:h2 "Members"]
   [:ul (for [m (:members @group-info)]
          ^{:key m} [:li m])]
   [:pre (str @group-info)]])

(defn ^:export init []
  (r/render [show-group] (get-element "show-group"))
  (let [group-id (->> js/window
                      .-location
                      .-pathname
                      (re-find #"/groups/([\d\w-]*)")
                      (#(% 1)))]
    (.log js/console "fetching group info")
    (GET (str "/api/v1/groups/" group-id)
         {:handler #(reset! group-info %)
          :error #(.log js/console %)
          :response-format :json
          :keywords? true})))
