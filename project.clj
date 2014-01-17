(def version "0.1.0-SNAPSHOT")

(def uberjar-name
  (str "target/cmr-es-spatial-plugin-" version "-standalone.jar"))

(def plugin-zip-name
  (str "target/cmr-es-spatial-plugin-" version ".zip"))

(defproject cmr-es-spatial-plugin version
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.elasticsearch/elasticsearch "0.90.7"]

                 ;; Version set to match elastic search numbers. Look in elasticsearch pom.xml
                 [log4j/log4j "1.2.17"]
                 [nasa-cmr/cmr-spatial "0.1.0-SNAPSHOT"]]

  :plugins [[lein-shell "0.3.0"]]

  :aot [cmr.es-spatial-plugin.StringMatchScript
        cmr.es-spatial-plugin.StringMatchScriptFactory
        cmr.es-spatial-plugin.SpatialScript
        cmr.es-spatial-plugin.SpatialScriptFactory
        cmr.es-spatial-plugin.SpatialSearchPlugin]


  :global-vars {*warn-on-reflection* true
                *assert* false}

  :profiles
  {:integration
   {:jvm-opts ["-Des.config=integration_test/elasticsearch.yml"
               "-Des.path.conf=integration_test"

               ;; uncomment to debug log4j
               ; "-Dlog4j.debug=true"

               ;; important to allow logging
               "-Des.foreground=true"]}

   :dev {:dependencies [[nasa-cmr/cmr-common "0.1.0-SNAPSHOT"]
                        [clojurewerkz/elastisch "1.3.0-rc2"]
                        [org.clojure/tools.namespace "0.2.4"]
                        [org.clojars.gjahad/debug-repl "0.3.3"]
                        [reiddraper/simple-check "0.5.3"]]
         :jvm-opts [;; important to allow logging to standard out
                    "-Des.foreground=true"]
         :source-paths ["src" "dev"]}}

  :aliases {;; Packages the spatial search plugin
            "package" ["do"
                       "clean,"
                       "uberjar,"
                       "shell" "zip" "-j" ~plugin-zip-name ~uberjar-name]

            ;; Packages and installs the plugin into the local elastic search vm
            "install-local" ["do"
                             "package,"
                             "shell" "../cmr-vms/elastic_local/install_plugin.sh" ~plugin-zip-name "spatialsearch-plugin"]

            "install-aws" ["do"
                           "package,"
                           ;; IP address is hard coded for now
                           "shell" "../cmr-vms/elastic_aws/install_plugin.sh" "54.193.23.62" ~plugin-zip-name "spatialsearch-plugin"]
                           })
