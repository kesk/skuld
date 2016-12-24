(ns skuld.core
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST context defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as response]
            [skuld.data-model :as data :refer [->Database]]
            [skuld.rest-api :refer [api-handler]])
  (:gen-class))

(def db (->Database data/query-conf))

(defn handle-group-request
  [req]
  (let [members (for [[k v] (:params req)
                      :when (and (not (s/blank? v))
                                 (re-matches #"member\d+" k))] (s/trim v))
        group-id (data/create-group
                   db (get-in req [:params "group_name"]) members)]
    (response/redirect (str "/groups/" group-id))))

(defroutes app-routes
  (GET "/" [] (response/resource-response "create_group.html"))
  (GET "/groups/:group-id" [group-id] (response/resource-response "groups.html"))
  (POST "/groups" [] handle-group-request)
  (context "/api/v1" [] api-handler)
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
