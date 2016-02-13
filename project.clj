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
                 [environ "1.0.2"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.2"]]
  :ring {:handler skuld.core/ring-handler}
  :main ^:skip-aot skuld.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
