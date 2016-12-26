(ns skuld.create-group.app
  (:require [skuld.common :refer [get-element]]
            [skuld.create-group.list-groups :as list-group]
            [reagent.core :as r]
            [clojure.string :as s]
            [ajax.core :refer [GET]]))

(defonce group-members (r/atom [""]))

(defn- remove-nth [col n]
  (into (vec (take n col)) (drop (inc n) col)))

(defn handle-event
  [state [event-name idx username]]
  (case event-name
    :remove (remove-nth state idx)))

(defn dispatch [event]
  (r/rswap! group-members handle-event event))

(defn print-group-members []
  [:pre (s/join "\n" (map-indexed #(str %1 ": " %2) @group-members))])

(defn on-form-name-change [idx element]
  (if (and (= (inc idx) (count @group-members))
           (not= "" (-> element .-target .-value)))
    (r/rswap! group-members conj ""))
  (r/rswap! group-members assoc idx (-> element .-target .-value)))

(defn group-member-input []
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
                                (dispatch [:remove idx]))}
               [:span {:class "glyphicon glyphicon-remove-sign"}]]]])
          @group-members)))

(defn create-group-form []
  [:div
   [:form {:action "/groups" :method "post"}
    [:div {:class "form-group"}
     [:label {:for "groupname"} "Group name:"]
     [:input {:name "groupname" :type "text"
              :id "groupname" :class "form-control"}]]
    [:p {:class "help-block"} "Input names of group members:"]
    [group-member-input group-members]
    [:button {:type "submit" :class "btn btn-default"} "Submit"]]
   [print-group-members]])

(defn ^:export init []
  (r/render [create-group-form] (get-element "app"))
  (list-group/init "group-list"))

