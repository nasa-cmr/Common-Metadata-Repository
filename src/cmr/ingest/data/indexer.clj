(ns cmr.ingest.data.indexer
  "Implements Ingest App datalayer access interface. Takes on the role of a proxy to indexer app."
  (:require [cmr.ingest.data :as data]
            [cmr.common.lifecycle :as lifecycle]
            [clojure.string :as string]
            [clj-http.client :as client]
            [cheshire.core :as  cheshire] 
            [cmr.common.log :as log :refer (debug info warn error)]
            [cmr.common.services.errors :as errors]))

(defn indexer-config
  "Returns default Indexer-DB Config."
  []
  (let [host "localhost"
        port 3004
        indexer-url (str "http://" host ":" port)]
    {:host host, :port port, :indexer-url indexer-url}))

(defn build-http-request-fn
  "Template used for building a request."
  [post-or-put-op indexer-url concept-attribs-json-str]
  {:method (keyword post-or-put-op)
   :url indexer-url
   :body concept-attribs-json-str
   :body-encoding "UTF-8"
   :content-type :json
   :socket-timeout 2000  ;; in milliseconds
   :conn-timeout 2000    ;; in milliseconds
   :accept :json})

(defn- indexer 
  "Submit a concept for indexing."
  [http-request]
  (let [response (client/request http-request) 
        status (:status response)]
    (when-not (= 201 status)
      (errors/internal-error! (str "Operation to index a concept failed. Indexer app response status code: "  status (str response))))))

(defn- delete-target-concept
  "Delete concept from index."
  [url concept-id revision-id]
  ;; currently mdb has just the force delete (all) oper hence discarding concept-id and revision-id
  (let [response (client/delete (str url "/" concept-id "/" revision-id)) 
        status (:status response)]
    (when-not (= 200 status)
      (errors/internal-error! (str "Delete concept operation failed. Indexer app response status code: "  status (str response))))))

;;; datalayer protocol to access indexer db
(defrecord Indexer-DB
  [config]
  
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  lifecycle/Lifecycle
  
  (start [this system]
         this)
  
  (stop [this system]
        this)
  
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  data/ConceptIndexStore
  
  (index-concept
    [this concept-id revision-id]              
    (let [{:keys [host port indexer-url]} (:config this)
          concept-attribs (into {} [[:concept-id concept-id] [:revision-id revision-id]])
          concept-attribs-json-str (cheshire/generate-string concept-attribs)
          http-request (build-http-request-fn "post" indexer-url concept-attribs-json-str)]
      (indexer http-request)))
  
  (delete-concept-from-index
    [this concept-id revision-id]
    (let [{:keys [indexer-url]} (:config this)]
      (delete-target-concept indexer-url concept-id revision-id))))

(defn create
  "Create proxy to indexer app."
  []
  (map->Indexer-DB {:config (indexer-config)}))       

