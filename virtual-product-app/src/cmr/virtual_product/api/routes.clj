(ns cmr.virtual-product.api.routes
  "Defines the HTTP URL routes for the application."
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [cmr.common-app.api.routes :as common-routes]
            [compojure.core :refer :all]
            [ring.middleware.json :as ring-json]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.common.api.errors :as errors]
            [cmr.common.services.errors :as serv-errors]
            [cmr.common.api.context :as context]
            [cmr.common.mime-types :as mt]
            [cmr.virtual-product.services.virtual-product-service :as vps]
            [cheshire.core :as json]
            [cmr.common.validations.json-schema :as js]
            [cmr.virtual-product.services.health-service :as hs]))

(def granule-entries-schema
  "Schema for the JSON request to the translate-granule-entries end-point"
  (js/parse-json-schema
    {:type :array
     :items {:type :object
             :properties {:concept-id {:type :string}
                          :entry-title {:type :string}
                          :granule-ur {:anyOf [{:type :string}
                                               {:type :null}]}}
             :required [:concept-id :entry-title :granule-ur]}}))

(defn- translate
  [context json-str]
  ;; Checks the json-str for validity as well as well-formedness
  (when-let [errors (seq (js/validate-json granule-entries-schema json-str))]
    (serv-errors/throw-service-errors :bad-request errors))
  (vps/translate-granule-entries context (json/parse-string json-str true)))

(defn- build-routes [system]
  (routes
    (context (:relative-root-url system) []
      (context "/foo" []
        (GET "/" {params :params headers :headers context :request-context}
          {:status 200
           :headers {"Content-Type" "text/plain"}
           :body "foo"}))

      (context "/translate-granule-entries" []
        (POST "/" {:keys [body content-type headers request-context]}
          (if (= (mt/mime-type->format content-type) :json)
            {:status 200
             :body (translate request-context (slurp body))}
            {:status 415
             :body (str "Unsupported content type [" content-type "]")})))

      (common-routes/health-api-routes hs/health))
    (route/not-found "Not Found")))

(defn make-api [system]
  (-> (build-routes system)
      (context/build-request-context-handler system)
      errors/invalid-url-encoding-handler
      errors/exception-handler
      handler/site
      ring-json/wrap-json-response))

