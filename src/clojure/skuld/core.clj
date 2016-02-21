(ns skuld.core
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [ANY GET POST defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as response]
            [skuld.api.api-router :refer [api-handler]]
            [skuld.data-model :as data])
  (:gen-class))

(defn handle-group-request
  [req]
  (let [group-id (data/create-group (get-in req [:params "group_name"]))]
    (response/redirect (str "/group/" group-id))))

(defroutes app-routes
  (GET "/" [] (response/resource-response "create_group.html"))
  (GET "/group" [] handle-group-request)
  (GET "/group/:group-id" [group-id] (response/resource-response "groups.html"))
  (POST "/group" [] handle-group-request)
  (GET "/redirect" [] (response/redirect "/group"))
  (ANY "/api/v1" [] api-handler)
  (GET "/hello/:n" [n] (str "Hello " n "!"))
  (route/resources "/"))

(defn- wrap-request-logging
  [handler]
  (fn [{:keys [request-method uri] :as req}]
    (let [resp (handler req)]
      (log/info
        (name request-method)
        (:status resp)
        (if-let [qs (:query-string req)]
          (str uri "?" qs) uri))
      resp)))

(def ring-handler
  (-> app-routes
      wrap-request-logging
      wrap-params
      wrap-session))

(defn -main
  [& args]
  (run-jetty ring-handler {:port 3000}))
