(defproject nasa-cmr/cmr-search-app "0.1.0-SNAPSHOT"
  :description "Provides a public search API for concepts in the CMR."
  :url "***REMOVED***projects/CMR/repos/cmr-search-app/browse"
  ;; Need the maven repo for the echo-orbits-java jar that isn't available in public maven repos.
  :repositories [["releases" "http://devrepo1.dev.echo.nasa.gov/data/dist/projects/echo/mavenrepo/"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [nasa-cmr/cmr-system-trace-lib "0.1.0-SNAPSHOT"]
                 [nasa-cmr/cmr-metadata-db-app "0.1.0-SNAPSHOT"]
                 [nasa-cmr/cmr-umm-lib "0.1.0-SNAPSHOT"]
                 [nasa-cmr/cmr-spatial-lib "0.1.0-SNAPSHOT"]
                 [nasa-echo/echo-orbits-java "0.1.5"]
                 [compojure "1.1.9"]
                 [ring/ring-core "1.3.1" :exclusions [clj-time]]
                 [ring/ring-json "0.3.1"]
                 [org.clojure/tools.reader "0.8.8"]
                 [org.clojure/tools.cli "0.3.1"]
                 [nasa-cmr/cmr-elastic-utils-lib "0.1.0-SNAPSHOT"]
                 [org.clojure/data.csv "0.1.2"]
                 [net.sf.saxon/Saxon-HE "9.5.1-6"]]
  :plugins [[lein-test-out "0.3.1"]
            [lein-exec "0.3.4"]]
  :repl-options {:init-ns user}
  :jvm-opts ["-XX:PermSize=256m" "-XX:MaxPermSize=256m"]
  :profiles
  {:dev {:dependencies [[org.clojure/tools.namespace "0.2.5"]
                        [org.clojars.gjahad/debug-repl "0.3.3"]
                        [criterium "0.4.3"]
                        [pjstadig/humane-test-output "0.6.0"]
                        ;; Must be listed here as metadata db depends on it.
                        [drift "1.5.2"]
                        [markdown-clj "0.9.47"]]
         :source-paths ["src" "dev" "test"]
         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]}
   :uberjar {:main cmr.search.runner
             :aot :all}}

  ;; Note this takes a while to run. We commit the files that are generated.
  :aliases {"generate-docs" ["exec" "-p" "./support/generate_docs.clj"]})



