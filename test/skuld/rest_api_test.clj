(ns skuld.rest-api-test
  (:require [clojure.data.json :as json]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [skuld.data-model :refer [GroupStorage]]
            [skuld.rest-api :refer [api-routes]]))

(defrecord MockDatabase []
  GroupStorage
  (get-group [d group-id]
    {:foo "bar"}))

(def mock-db (->MockDatabase))

(deftest get-group-returns-json
  (is (= (:body ((api-routes mock-db) (mock/request :get "/groups/abc")))
         (json/write-str {:foo :bar}))))
