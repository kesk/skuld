(ns skuld.rest-api
  (:require [camel-snake-kebab.core :refer [->kebab-case]]
            [clojure.data.json :as json]
            [compojure.core :refer [ANY GET routes]]
            [environ.core :refer [env]]
            [liberator.core :refer [defresource]]
            [liberator.dev :refer [wrap-trace]]
            [ring.util.response :as res]
            [skuld.data-model :as groups]
            [clojure.tools.logging :as log]
            ))

(def database (groups/map->SQLiteDatabase groups/db-spec))

(defresource groups-resource [db id]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (if (nil? id)
                 (json/write-str (groups/list-groups db))
                 (json/write-str (groups/get-group db id)))))

(defresource expenses-resourse [db group-id]
  :allowed-methods [:post :get]
  :available-media-types ["application/json" "application/clojure"]
  :handle-ok (fn [ctx]
               (groups/get-group-expenses database group-id))
  :handle-created (fn [ctx] {:id (::expense-id ctx)})
  :post! (fn [ctx]
           (let [body (slurp (-> ctx :request :body))
                 data (json/read-str body :key-fn keyword)
                 group-id (get-in ctx [:request :params :id])
                 id (groups/create-expense db group-id
                                           (:payed-by data)
                                           (:shared-with data)
                                           (:amount data))]
             {::expense-id id})))

(defresource dept-resource [db group-id]
  :allowed-methods [:get]
  :available-media-types ["application/json" "application/clojure"]
  :handle-ok (fn [ctx]
               (let [media-type (get-in ctx [:representation :media-type])]
                 (condp = media-type
                   "application/json" (into [] (groups/get-group-dept db group-id))
                   (into [] (groups/get-group-dept db group-id))))))

(defn- hello-world [request]
  (-> (res/response "Hello, api!")
      (res/content-type "*/*")))

(defn api-routes
  [db]
  (routes
    (GET "/hello" [] hello-world)
    (ANY "/groups" [] (groups-resource db nil))
    (ANY "/groups/:id" [id] (groups-resource db id))
    (ANY "/groups/:id/expenses" [id] (expenses-resourse db id))
    (ANY "/groups/:id/dept" [id] (dept-resource db id))))

(def api-handler
  (-> (api-routes database)
      (wrap-trace :header :ui)))
