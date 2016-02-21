(ns skuld.api.api-router
  (:require [clojure.data.json :as json]
            [clojure.pprint :as pp]
            [compojure.core :refer [ANY defroutes]]
            [liberator.core :refer [defresource]]
            [ring.util.response :as res]))

(defresource groups-resource [id]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (with-out-str (pp/pprint (json/write-str ctx)))))

(defn- hello-world [request]
  (-> (res/response "Hello, api!")
      (res/content-type "text/plain")))

(defroutes api-handler
  (ANY "/groups/:id" [id] (groups-resource id)))
