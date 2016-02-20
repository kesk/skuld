(ns skuld.core
  (:require [clojure.java.jdbc :refer [insert! query]]
            [clojure.tools.logging :as log]
            [compojure.core :as compojure :refer [ANY GET defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as response :refer [content-type response]]
            [skuld.api.api-router :refer [api-handler]]
            [skuld.web-app.groups :as groups])
  (:gen-class))

(def sqlite-db {:classname "org.sqlite.JDBC"
                :subprotocol "sqlite"
                :subname "database.sqlite"})

(defn handler [request]
  (-> (response "Hello World")
      (content-type "text/plain")))

(def user {:username "John"
           :email "my@email.com"})

(defn add-user [user]
  (insert! sqlite-db :users user))

(defn list-users []
  (query sqlite-db "select * from users"))

(defroutes app-routes
  (GET "/" [] (response/resource-response "create_group.html"))
  (compojure/context "/groups" [] groups/routes)
  (ANY "/api/v1" [] api-handler)
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
