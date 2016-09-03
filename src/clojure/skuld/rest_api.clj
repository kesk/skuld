(ns skuld.rest-api
  (:require [clojure.data.json :as json]
            [compojure.core :refer [ANY defroutes]]
            [liberator.core :refer [defresource]]
            [ring.util.response :as res]
            [skuld.data-model :as groups]))

(defresource groups-resource [id]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (json/write-str (groups/get-group id))))

(defresource expenses-resourse [group-id]
  :allowed-methods [:post :get]
  :available-media-types ["application/json" "application/clojure"]
  :handle-ok (fn [ctx]
               (let [media-type (-> ctx :representation :media-type)]
                 (condp = media-type
                   "application/json" "{\"message\":\"EXPENSES\"}"
                   "application/clojure" {:message "EXPENSES"}))))

(defn- hello-world [request]
  (-> (res/response "Hello, api!")
      (res/content-type "*/*")))

(defroutes api-routes
  (ANY "/groups/:id" [id] (groups-resource id))
  (ANY "/groups/:id/expenses" [id] (expenses-resourse id)))
