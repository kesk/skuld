(ns skuld.web-app.groups
  (:require [compojure.core :refer [GET defroutes]]
            [ring.util.response :as resp]))

(defroutes routes
  (GET "/" [] (resp/resource-response "groups.html")))
