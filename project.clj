(defproject skuld "latest"
  :description "Tool for keeping track of group depts."
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.1"]
                 [ring/ring-defaults "0.2.3"]
                 [ring/ring-jetty-adapter "1.5.1"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.xerial/sqlite-jdbc "3.16.1"]
                 [compojure "1.5.2"]
                 [liberator "0.14.1"]
                 [yesql "0.5.3"]
                 [clj-time "0.13.0"]
                 [camel-snake-kebab "0.4.0"]
                 [environ "1.1.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [reagent "0.6.1"]
                 [selmer "1.10.7"]
                 [org.clojure/data.json "0.2.6"]
                 [cljs-ajax "0.5.9"]
                 [ring-logger "0.7.7"]
                 [secretary "1.2.3"]
                 [alumbra "0.2.2"]]
  :bower-dependencies [[bootstrap "4.0.0-alpha.6"
                        font-awesome "4.7.0"]]
  :plugins [[lein-ring "0.11.0"]
            [lein-ancient "0.6.10"]
            [lein-environ "1.1.0"]
            [lein-figwheel "0.5.10"]
            [lein-cljsbuild "1.1.6"]
            [lein-bower "0.5.2"]
            [lein-pprint "1.1.2"]]
  :ring {:handler skuld.core/ring-handler}
  :main ^:skip-aot skuld.core
  :source-paths ["src/clojure"]
  :target-path "target/%s"
  :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs/"]
  ;:hooks [leiningen.cljsbuild]

  :cljsbuild {:builds
              {:create-group
               {:source-paths ["src/clojurescript"]
                :compiler {:main skuld.create-group.app
                           :asset-path "/cljs/create_group_out"
                           :output-to "resources/public/cljs/create_group.js"
                           :output-dir "resources/public/cljs/create_group_out"}}
               :groups
               {:source-paths ["src/clojurescript"]
                :compiler {:main skuld.show-group.app
                           :asset-path "/cljs/groups_out"
                           :output-to "resources/public/cljs/groups.js"
                           :output-dir "resources/public/cljs/groups_out"}}}}

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler skuld.core/ring-handler}

  :profiles
  {:dev {:dependencies [[ring/ring-mock "0.3.0"]]
         :env {:environment "dev"
               :database-url "database.sqlite"}
         :ring {:auto-reload? true}
         :cljsbuild
         {:builds {:create-group
                   {:figwheel {:on-jsload "skuld.create-group.app/init"}
                    :compiler {:optimizations :none}}
                   :groups
                   {:figwheel {:on-jsload "skuld.show-group.app/init"}
                    :compiler {:optimizations :none}}}}}

   :prod {:env {:environment "prod"
                :database-url "database.sqlite"}
          :cljsbuild
          {:builds
           {:create-group
            {:compiler {:optimizations :advanced}}
            :groups
            {:compiler {:optimizations :advanced}}}}}

   :prod-debug {:env {:environment "prod"
                      :database-url "database.sqlite"}
                :cljsbuild
                {:builds
                 {:create-group
                  {:compiler {:optimizations :advanced
                              :pretty-print true
                              :pseudo-names true
                              :source-map "resources/public/cljs/groups.js.map"}}
                  :groups
                  {:compiler {:optimizations :advanced
                              :pretty-print true
                              :pseudo-names true
                              :source-map "resources/public/cljs/create_group.js.map"}}}}}

   :uberjar {:aot :all}}

  :test-selectors {:default (constantly true)
                   :it :it})
