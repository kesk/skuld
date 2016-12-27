(defproject skuld "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.xerial/sqlite-jdbc "3.15.1"]
                 [compojure "1.5.1"]
                 [liberator "0.14.1"]
                 [yesql "0.5.3"]
                 [clj-time "0.12.2"]
                 [camel-snake-kebab "0.4.0"]
                 [environ "1.1.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [reagent "0.6.0"]
                 [selmer "1.10.3"]
                 [org.clojure/data.json "0.2.6"]
                 [cljs-ajax "0.5.8"]
                 [ring-logger "0.7.6"]]
  :bower-dependencies [[bootstrap "~3.3.7"]]
  :plugins [[lein-ring "0.10.0"]
            [lein-ancient "0.6.10"]
            [lein-environ "1.1.0"]
            [lein-figwheel "0.5.8"]
            [lein-cljsbuild "1.1.5"]
            [lein-bower "0.5.2"]
            [lein-pprint "1.1.2"]]
  :ring {:handler skuld.core/ring-handler}
  :main ^:skip-aot skuld.core
  :source-paths ["src/clojure"]
  :target-path "target/%s"
  :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs/"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds
              {:create-group
               {:source-paths ["src/clojurescript"]
                :compiler {:asset-path "/cljs/create_group_out"
                           :output-to "resources/public/cljs/create_group.js"
                           :output-dir "resources/public/cljs/create_group_out"}}
               :groups
               {:source-paths ["src/clojurescript"]
                :compiler {:asset-path "/cljs/groups_out"
                           :output-to "resources/public/cljs/groups.js"
                           :output-dir "resources/public/cljs/groups_out"}}}}
  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler skuld.core/ring-handler}

  :profiles
  {:dev {:dependencies [[ring/ring-mock "0.3.0"]]
         :env {:database-url "database.sqlite"}
         :ring {:auto-reload? true}
         :cljsbuild
         {:builds {:create-group
                   {:figwheel {:on-jsload "skuld.create-group.app/init"}
                    :compiler {:main skuld.create-group.app
                               :optimizations :none}}
                   :groups
                   {:figwheel {:on-jsload "skuld.show-group.app/init"}
                    :compiler {:main skuld.show-group.app
                               :optimizations :none}}}}}
   :prod {:cljsbuild
          {:builds
           {:create-group
            {:compiler {:optimizations :advanced}}
            :groups
            {:compiler {:optimizations :advanced}}}}}
   :uberjar {:aot :all}}

  :test-selectors {:default (constantly true)
                   :it :it})
