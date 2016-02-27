(ns skuld.api.api-router
  (:require [clojure.data.json :as json]
            [compojure.core :refer [ANY defroutes]]
            [liberator.core :refer [defresource]]
            [ring.util.response :as res]
            [skuld.data-model :as groups]))

(defresource groups-resource [id]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (json/write-str (groups/get-group id))))

(defn- hello-world [request]
  (-> (res/response "Hello, api!")
      (res/content-type "*/*")))

(defroutes api-handler
  (ANY "/groups/:id" [id] (groups-resource id)))
