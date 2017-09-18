(ns clojure-app.core-test
  (:require [clojure.test :refer :all]
            [clojure-app.core :refer :all]
            [ring.mock.request]
            [expectations :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

;(expect (app (request :get "/users")) (is (= (:status response) 200
; (is (= (get-in response [:headers "Content-Type"]) "application-json"))