(ns skuld.rest-api
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [ANY defroutes routes]]
            [liberator.core :refer [defresource]]
            [liberator.dev :refer [wrap-trace]]
            [ring.util.response :as res]
            [skuld.data-model :as groups]))

(def database (groups/->Database))

(defresource groups-resource [db id]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (json/write-str (groups/get-group db id))))

(defresource expenses-resourse [group-id]
  :allowed-methods [:post :get]
  :available-media-types ["application/json" "application/clojure"]
  :handle-ok (fn [ctx]
               (groups/get-group-expenses database group-id))
  :handle-created (fn [ctx] {:id (::expense-id ctx)})
  :post! (fn [ctx]
           (let [body (slurp (-> ctx :request :body))
                 data (json/read-str body :key-fn keyword)
                 group-id (get-in ctx [:request :params :id])
                 id (groups/add-shared-expense group-id (:payed-by data)
                                               (:amount data) (:shared-with data))]
             {::expense-id id})))

(defresource dept-resource [group-id]
  :allowed-methods [:get]
  :available-media-types ["application/json" "application/clojure"]
  :handle-ok (fn [ctx]
               (let [media-type (get-in ctx [:representation :media-type])]
                 (condp = media-type
                   "application/json" (into [] (groups/get-group-dept database group-id))
                   (into [] (groups/get-group-dept database group-id))))))

(defn- hello-world [request]
  (-> (res/response "Hello, api!")
      (res/content-type "*/*")))

(defn api-routes
  [db]
  (routes
    (ANY "/groups/:id" [id] (groups-resource db id))
    (ANY "/groups/:id/expenses" [id] (expenses-resourse id))
    (ANY "/groups/:id/dept" [id] (dept-resource id))))

(def api-handler
  (-> (api-routes database)
      (wrap-trace :header :ui)))
