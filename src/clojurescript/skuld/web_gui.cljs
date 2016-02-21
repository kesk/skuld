(ns skuld.web-gui
  (:require [reagent.core :as r]
            [clojure.string :as s]
            [ajax.core :refer [GET]]))

(defonce value (r/atom "foo"))

(defn get-element [id]
  (.getElementById js/document id))

(defonce group-members (r/atom [""]))

(defn print-group-members [members]
  [:pre (s/join "\n" (map-indexed #(str %1 ": " %2) members))])

(defn on-form-name-change [idx element]
  (if (and (= (inc idx) (count @group-members))
           (not= "" (-> element .-target .-value)))
    (swap! group-members conj ""))
  (swap! group-members assoc idx (-> element .-target .-value)))

(defn- remove-nth [col n]
  (into (vec (take (dec n) col)) (drop n col)))


(defn group-member-form []
  (into [:div]
        (map-indexed
          (fn [idx member-name]
            [:div {:class "form-group"}
             [:div {:class "input-group"}
              [:input {:name (str "member" idx)
                       :type "text"
                       :value member-name
                       :on-change (partial on-form-name-change idx)
                       :placeholder "Name"
                       :class "form-control"}]
              [:a {:class "input-group-addon"
                   :on-click #(if (< 1 (count @group-members))
                                (swap! group-members remove-nth (inc idx)))}
               [:span {:class "glyphicon glyphicon-remove-sign"}]]]])
          @group-members)))

(defn create-group-form []
  [:div
    [:form {:action "/group" :method "post"}
     [:div {:class "form-group"}
      [:label {:for "group-name"} "Group name:"]
      [:input {:name "group_name" :type "text"
               :id "group_name" :class "form-control"}]]
     [:p {:class "help-block"} "Input names of group members:"]
     [group-member-form group-members]
     [:button {:type "submit" :class "btn btn-default"} "Submit"]]
   [print-group-members @group-members]])

(defn init-start [match]
  (r/render-component [create-group-form] (get-element "app")))

(defn show-group [group-id]
  [:p (str "Nice group! " group-id)])

(defn init-group [match]
  (r/render-component [show-group (match 1)] (get-element "show-group")))

; Some routing (sort of)
(condp re-matches (-> js/window .-location .-pathname)
  #"/" :>> init-start
  #"/group/(.*)" :>> init-group)

(GET "/hello/sebbe"
     {:handler #(.log js/console (str %))
      :error #(.log js/console (str %))})

