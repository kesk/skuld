(ns skuld.api.api-router
  (:require [compojure.core :refer [ANY defroutes]]
            [liberator.core :refer [defresource]]
            [ring.util.response :as res]))

(defresource groups-resource
  []
  :available-media-types ["text/plain"]
  :handle-ok "Hello, groups!")

(defn- hello-world [request]
  (-> (res/response "Hello, api!")
      (res/content-type "text/plain")))

(defroutes api-handler
  (ANY "/" [] hello-world))
