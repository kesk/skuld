(ns skuld.core
  (:require [clojure.string :as s]
            [compojure.core :refer [GET POST context defroutes]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :as response]
            [selmer.parser :as selmer]
            [skuld.data-model :as data :refer [->Database]]
            [skuld.rest-api :refer [api-handler]])
  (:gen-class))

(def db (->Database data/query-conf))

(defn handle-group-request
  [req]
  (let [members (for [[k v] (:params req)
                      :when (and (not (s/blank? v))
                                 (re-matches #"member\d+" (name k)))] (s/trim v))
        group-id (data/create-group
                   db (get-in req [:params :groupname]) members)]
    (response/redirect (str "/groups/" group-id))))

(defn- render-template
  ([filename]
   (render-template filename "Skuld"))
  ([filename title]
   (selmer/render-file filename {:title title})))

(defroutes app-routes
  (GET "/" [] (render-template "create_group.html"))
  (GET "/groups" [] (render-template "groups.html"))
  (POST "/groups" [] handle-group-request)
  (context "/api/v1" [] api-handler)
  (GET "/hello/:n" [n] (str "Hello " n "!"))
  (route/resources "/"))

(def ring-handler
  (-> app-routes
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-with-logger))

(defn -main
  [& args]
  (if (= "dev" (:environment env)) (selmer.parser/cache-off!))
  (run-jetty ring-handler {:port 3000}))
