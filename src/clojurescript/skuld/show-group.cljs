(ns skuld.show-group
  (:require [skuld.common :refer [get-element]]
            [reagent.core :as r]
            [ajax.core :refer [GET]]))

(defonce group-info (r/atom nil))

(defn show-group [group-id]
  [:div
   [:p (str "Nice group! " group-id)]
   [:pre (str @group-info)]])

(defn ^:export init []
  (let [group-id (->> js/window
                      .-location
                      .-pathname
                      (re-find #"/groups/([\d\w-]*)")
                      (#(% 1)))]
    (GET (str "/api/v1/groups/" group-id)
         {:handler #(reset! group-info %)
          :error #(.log js/console %)
          :response-format :json
          :keywords? true})
    (r/render-component [show-group group-id] (get-element "show-group"))))

(init)
