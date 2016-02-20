(defproject skuld "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [compojure "1.4.0"]
                 [liberator "0.13"]
                 [yesql "0.5.1"]
                 [clj-time "0.11.0"]
                 [camel-snake-kebab "0.3.2"]
                 [environ "1.0.2"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.6.0-alpha"]
                 [selmer "1.0.0"]
                 #_[ch.qos.logback/logback-classic "1.1.1"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.2"]
            [lein-figwheel "0.5.0-6"]
            [lein-bower "0.5.1"]
            [lein-cljsbuild "1.1.2"]]
  :ring {:handler skuld.core/ring-handler}
  :main ^:skip-aot skuld.core
  :source-paths ["src/clojure"]
  :target-path "target/%s"
  :clean-targets ^{:protect false} [:target-path "out" "resources/public/cljs/"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/clojurescript"]
                        :figwheel true
                        :compiler {:main skuld.web-gui
                                   :asset-path "cljs/out"
                                   :output-to "resources/public/cljs/main.js"
                                   :output-dir "resources/public/cljs/out"}}
                       {:id "prod"
                        :source-paths ["src/clojurescript"]
                        :compiler {:main skuld.web-gui
                                   :output-to "resources/public/cljs/main_prod.js"
                                   :output-dir "resources/public/cljs/prod_out"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :source-map "resources/public/cljs/main_prod.js.map"
                                   }}
                       ]}
  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler skuld.core/ring-handler}
  :profiles {:uberjar {:aot :all}})
