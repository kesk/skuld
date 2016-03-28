(ns skuld.core
  (:require [skuld.common :refer [get-element]]
            [skuld.create-group :as create-group]
            [skuld.show-group :as show-group]
            [reagent.core :as r]))

; Some routing (sort of)
#_(condp re-matches (-> js/window .-location .-pathname)
  #"/" :>> create-group/init
  #"/groups/(.*)" :>> show-group/init)

