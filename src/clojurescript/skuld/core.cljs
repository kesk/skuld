(ns skuld.core
  (:require [skuld.create-group :as create-group]
            [skuld.show-group :as show-group]))

; Some routing (sort of)
(condp re-matches (-> js/window .-location .-pathname)
  #"/" :>> create-group/init
  #"/groups/(.*)" :>> show-group/init)
