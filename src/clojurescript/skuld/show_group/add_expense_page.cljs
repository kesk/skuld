(ns skuld.show-group.add-expense-page
  (:require [skuld.common :refer [get-element]]
            [skuld.show-group.state :refer [app-state dispatch!]]
            [reagent.core :as r]
            [ajax.core :refer [GET POST]]
            [clojure.string :as s]
            [cljs.pprint :refer [pprint]]))

(def log js/console.log)

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn expenses []
  (:group-expenses @app-state))

(defn total-expenses []
  (->> @(r/track expenses)
      (map :amount)
      (reduce + 0)))

(defn show-group []
  (let [total @(r/track total-expenses)]
    [:div
     [:p (str "Group id: " (-> @app-state :group-info :id))]
     [:h1 (:name @app-state)]
     [:h2 "Members"]
     [:ul (for [m (-> @app-state :group-info :members)]
            ^{:key m} [:li m])]
     [:p "Den här gruppen har spenderat totalt "
      [:span {:class "font-weight-bold"} total] " kr."]
     [:p>a {:href "#/expenses"} "Lista alla utgifter"]]))

(defn debug-state []
  [:div
   [:button {:class "btn btn-info"
             :style {:margin-top "5px"}
             :type "button"
             :data-toggle "collapse"
             :data-target "#debugState"}
    "Show debug info"]
   [:div {:class "collapse"
          :id "debugState"}
    [:pre {:class "card card-block"}
     (with-out-str (pprint @app-state))]]])

(def el-value #(-> % .-target .-value))

(def is-float (comp not js/window.isNaN js/window.parseFloat))

(defn amount-input []
  (let [amount (r/track #(-> @app-state :expense-form :amount))
        valid-amount #(or (empty? %1) (is-float %1))
        error-class (r/track #(if (not (valid-amount @amount)) "has-error"))
        on-change #(dispatch! [:change-amount (el-value %)])]
    (fn []
      [:div {:class (s/join " " ["form-group" @error-class])}
       [:label {:class "control-label" :for "amount"} "Pengar"]
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

(defn log-error [error]
  (log (str "Error: " error)))

(defn submit-expense-form [e]
  (let [clj->json-str (comp js/window.JSON.stringify clj->js)
        form-data (prepare-formdata (:expense-form @app-state))]
    (if (not (validate-form-data form-data))
      (log "Validation error")
      (do
        (log (clj->json-str form-data))
        (POST (str "/api/v1/groups/" (-> @app-state :group-info :id) "/expenses" )
              {:params form-data
               :format :json
               :handler #(log "data posted:" (clj->js %))
               :error-handler log-error})))))

(defn expense-form []
  (let [members (r/track #(-> @app-state :group-info :members))
        shared-with (r/track #(-> @app-state :expense-form :shared-with))]
    (fn []
      [:div {:class "card"}
       [:h3 {:class "card-header"} "Lägg till utgift"]
       [:div {:class "card-block"}
        [:div {:class "form-group"}
         [:label {:for "payed_by"} "Vem"]
         [:select#payed-by {:class "form-control"
                            :on-change #(dispatch! [:change-payed-by (el-value %)])}
          (for [m @members] ^{:key m} [:option m])]]
        [amount-input]
        [:label "Delad med"]
        [:div {:class "form-group"}
         (for [[n c] @shared-with]
           ^{:key n} [:div {:class "form-check"}
                      [:label {:class "form-check-label"}
                       [:input {:class "form-check-input"
                                :type "checkbox"
                                :name "shared_with"
                                :value n
                                :on-change #(dispatch! [:shared-with (el-value %)])
                                :checked c}] (str " " n)]])]
        [:button#submit-expense {:class "btn btn-primary"
                                 :on-click submit-expense-form}
         "Add expense"]]])))

(defn home []
  [:div
   [show-group]
   [expense-form]
   [debug-state]])