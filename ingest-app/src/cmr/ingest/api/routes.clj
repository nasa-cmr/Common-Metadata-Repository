(ns cmr.ingest.api.routes
  "Defines the HTTP URL routes for the application."
  (:require [compojure.route :as route]
            [compojure.core :refer :all]
            [ring.middleware.json :as ring-json]
            [ring.middleware.params :as params]
            [ring.middleware.nested-params :as nested-params]
            [ring.middleware.keyword-params :as keyword-params]
            [cmr.ingest.api.multipart :as mp]
            [clojure.stacktrace :refer [print-stack-trace]]
            [cmr.common.mime-types :as mt]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.common.api.errors :as api-errors]
            [cmr.ingest.services.jobs :as jobs]
            [cmr.acl.core :as acl]
            [cmr.ingest.services.ingest-service :as ingest]
            [cmr.system-trace.http :as http-trace]
            [cmr.ingest.api.provider :as provider-api]
            [cmr.ingest.api.ingest :as ingest-api]
            [cmr.ingest.api.translation :as translation-api]
            [cmr.common-app.api.routes :as common-routes]

            [cmr.common-app.api-docs :as api-docs]))

(defn- build-routes [system]
  (routes
    (context (get-in system [:ingest-public-conf :relative-root-url]) []
      provider-api/provider-api-routes

      ;; Add routes for translating metadata formats
      translation-api/translation-routes

      ;; Add routes to create, update, delete, validate concepts
      ingest-api/ingest-routes

      ;; Add routes for API documentation
      (api-docs/docs-routes (get-in system [:ingest-public-conf :protocol])
                            (get-in system [:ingest-public-conf :relative-root-url])
                            "public/ingest_index.html")

      ;; add routes for managing jobs
      (common-routes/job-api-routes
        (routes
          (POST "/reindex-collection-permitted-groups" {:keys [headers params request-context]}
            (acl/verify-ingest-management-permission request-context :update)
            (jobs/reindex-collection-permitted-groups request-context)
            {:status 200})
          (POST "/reindex-all-collections" {:keys [headers params request-context]}
            (acl/verify-ingest-management-permission request-context :update)
            (jobs/reindex-all-collections request-context)
            {:status 200})
          (POST "/cleanup-expired-collections" {:keys [headers params request-context]}
            (acl/verify-ingest-management-permission request-context :update)
            (jobs/cleanup-expired-collections request-context)
            {:status 200})))

      ;; add routes for accessing caches
      common-routes/cache-api-routes

      ;; add routes for checking health of the application
      (common-routes/health-api-routes ingest/health))

    (route/not-found "Not Found")))

(defn default-error-format-fn
  "Determine the format that errors should be returned in based on the default-format
  key set on the ExceptionInfo object passed in as parameter e. Defaults to json if
  the default format has not been set to :xml."
  [_request e]
  (get mt/format->mime-type (:default-format (ex-data e)) mt/json))

(defn make-api [system]
  (-> (build-routes system)
      acl/add-authentication-handler
      (http-trace/build-request-context-handler system)
      keyword-params/wrap-keyword-params
      nested-params/wrap-nested-params
      api-errors/invalid-url-encoding-handler
      mp/wrap-multipart-params
      (api-errors/exception-handler default-error-format-fn)
      common-routes/pretty-print-response-handler
      params/wrap-params))

