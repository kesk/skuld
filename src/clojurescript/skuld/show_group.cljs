(ns skuld.show-group
  (:require [skuld.common :refer [get-element]]
            [cljs.pprint :refer [pprint]]
            [goog.dom :as dom]
            [goog.events :as events]
            [reagent.core :as r]
            [ajax.core :refer [GET]]
            [clojure.string :as s]))

(def log js/console.log)

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defonce app-state (r/atom {:expense-form {:payed-by ""
                                           :amount ""
                                           :shared-with {}}}))

(defn event-handler [state [event-name value]]
  (case event-name
    :group-info (-> state
                    (update :group-info merge (update value :members sort))
                    (assoc-in [:expense-form :shared-with]
                              (into {} (map #(vec [% true]) (:members value))))
                    (assoc-in [:expense-form :payed-by] (first (:members value))))
    :shared-with (update-in state [:expense-form :shared-with value] not)
    :change-amount (assoc-in state [:expense-form :amount] value)
    :change-payed-by (assoc-in state [:expense-form :payed-by] value)
    state))

(defn dispatch [e]
  (r/rswap! app-state event-handler e))

(def group-id
  (->> js/window
       .-location
       .-pathname
       (re-find #"/groups/([\d\w-]*)")
       (#(% 1))))

(defn show-group []
  [:div
   [:p (str "Group id: " (-> @app-state :group-info :id))]
   [:h1 (:name @app-state)]
   [:h2 "Members"]
   [:ul (for [m (-> @app-state :group-info :members)]
          ^{:key m} [:li m])]
   [:pre (with-out-str (pprint @app-state))]])

(def el-value #(-> % .-target .-value))

(def is-float (comp not js/window.isNaN js/window.parseFloat))

(defn amount-input []
  (let [amount (r/track #(-> @app-state :expense-form :amount))
        valid-amount #(or (empty? %1) (is-float %1))
        error-class (r/track #(if (not (valid-amount @amount)) "has-error"))
        on-change #(dispatch [:change-amount (el-value %)])]
    (fn []
      [:div {:class (s/join " " ["form-group" @error-class])}
       [:label {:class "control-label" :for "amount"} "Amount"]
       [:input {:name "amount" :type "text"
                :id "group_name" :class "form-control"
                :on-change on-change
                :value @amount}]])))

(defn prepare-formdata [data]
  (-> data
      (update :shared-with #(->> %
                                 (filter (comp true? second))
                                 (map first)))
      (update :amount js/window.parseFloat)))

(defn validate-form-data [data]
  (and (is-float (:amount data))
       (every? false? (map empty? (vals (select-keys data [:payed-by :shared-with]))))))

(defn submit-expense-form [e]
  (let [clj->json-str (comp js/window.JSON.stringify clj->js)
        form-data (prepare-formdata (:expense-form @app-state))]
    (if (validate-form-data form-data)
      (log (clj->json-str form-data))
      (log "Validation error"))))

(defn expense-form []
  (let [members (r/track #(-> @app-state :group-info :members))
        shared-with (r/track #(-> @app-state :expense-form :shared-with))]
    (fn []
      [:div
       [:h3 "Add expense"]
       [:div {:class "form-group"}
        [:label {:for "payed_by"} "Payed by"]
        [:select#payed-by {:class "form-control"
                           :on-change #(dispatch [:change-payed-by (el-value %)])}
         (for [m @members] ^{:key m} [:option m])]]
       [amount-input]
       [:label "Shared with"]
       [:div {:class "form-group"}
        (for [[n c] @shared-with]
          ^{:key n} [:label {:class "checkbox-inline"}
                     [:input {:type "checkbox"
                              :name "shared_with"
                              :value n
                              :on-change #(dispatch [:shared-with (el-value %)])
                              :checked c}] n])]
       [:button#submit-expense {:class "btn btn-default"
                                :on-click submit-expense-form}
        "Add expense"]])))

(defn app []
  [:div
   [show-group]
   [expense-form]])

(defn get-group-info []
  (GET (str "/api/v1/groups/" group-id)
       {:handler #(dispatch [:group-info %])
        :error #(.log js/console %)
        :response-format :json
        :keywords? true}))

(defn ^:export init []
  (get-group-info)
  (r/render [app] (get-element "app")))
