(ns cmr.metadata-db.int-test.delete-concept-test
  "Contains integration tests for deleting concepts. Tests delete with various configurations including
  checking for proper error handling."
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [cheshire.core :as cheshire]
            [cmr.metadata-db.int-test.utility :as util]
            [cmr.metadata-db.data.messages :as messages]))

;;; fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def num-revisions 3) ; number of times the first concept will be saved

(def concept1-id "C1000000000-PROV1")

(defn setup-database-fixture
  "Load the database with test data."
  [f]
  ;; setup database
  (let [concept1 (util/concept)
        concept2 (merge concept1 {:concept-id "C2-PROV1" :native-id "some other id"})]
    (dorun (repeatedly num-revisions #(util/save-concept concept1)))
    
    (util/save-concept concept2))
  
  (f)
  
  ;; clear out the database
  (util/reset-database))

(use-fixtures :once setup-database-fixture)

;;; tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest mdb-delete-concept-test
  "Delete a concept and check the revision id of the tombstone."
  (let [{:keys [status revision-id]} (util/delete-concept concept1-id)]
    (is (= status 200))
    (is (= revision-id num-revisions))))

(deftest mdb-delete-concept-with-valid-revision
  "Delete a concept while specifying a valid revision."
  (let [{:keys [status revision-id]} (util/delete-concept concept1-id num-revisions)]
    (is (= status 200))
    (is (= revision-id num-revisions))))

(deftest mdb-delete-concept-with-invalid-revision
  "Delete a concept while specifying an invalid revision."
  (let [{:keys [status]} (util/delete-concept concept1-id (+ num-revisions 10))]
    (is (= status 409))))

(deftest mdb-fail-to-delete-missing-concept
  "Attempt to delete a concept that does not exist and verify that we get a 404."
  (let [{:keys [status revision-id error-messages]} (util/delete-concept "C1-NON-EXISTENT-PROVIDER")]
    (is (= status 404))
    (is (= error-messages [(format messages/concept-does-not-exist-msg "C1-NON-EXISTENT-PROVIDER")]))))

(deftest mdb-repeated-calls-to-delete-get-same-revision
  "Delete a concept repeatedly and verify that the revision does not change."
  (let [concept-id concept1-id
        tombstone-revision-id (:revision-id (util/delete-concept concept-id))]
    (dorun (repeatedly 3 #(util/delete-concept concept-id)))
    (let [final-revision-id (:revision-id (util/delete-concept concept-id))]
      (is (= tombstone-revision-id final-revision-id)))))
