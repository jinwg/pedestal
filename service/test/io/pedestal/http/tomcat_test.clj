; Copyright 2013 Relevance, Inc.
; Copyright 2014 Cognitect, Inc.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(ns io.pedestal.http.tomcat-test
  (:use clojure.test
        io.pedestal.http.tomcat)
  (:require [clj-http.client :as http]
            [clojure.edn]
            [io.pedestal.interceptor.helpers :refer [defhandler handler]]
            [io.pedestal.http.servlet :as servlet]
            [io.pedestal.http.impl.servlet-interceptor :as servlet-interceptor]))

(defhandler hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn content-type-handler [content-type]
  (handler
   (fn [_]
     {:status  200
      :headers {"Content-Type" content-type}
      :body    ""})))

(defhandler echo-handler [request]
  {:status 200
   :headers {"request-map" (str (dissoc request
                                        :body
                                        :servlet
                                        :servlet-request
                                        :servlet-response
                                        :servlet-context
                                        :pedestal.http.impl.servlet-interceptor/protocol
                                        :pedestal.http.impl.servlet-interceptor/async-supported?))}
   :body (:body request)})

(defn tomcat-server
  [app options]
  (server {:io.pedestal.http/servlet (servlet/servlet :service (servlet-interceptor/http-interceptor-service-fn [app]))}
          (assoc options :join? false)))

(defmacro with-server [app options & body]
  `(let [server# (tomcat-server ~app ~options)]
     (try
       ((:start-fn server#))
       ~@body
       (finally ((:stop-fn server#))))))

(deftest test-run-tomcat
  (testing "HTTP server"
    (with-server hello-world {:port 4347}
      (let [response (http/get "http://localhost:4347")]
        (is (= (:status response) 200))
        (is (.startsWith ^String (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World")))))

  (testing "default character encoding"
    (with-server (content-type-handler "text/plain") {:port 4348}
      (let [response (http/get "http://localhost:4348")]
        (is (.contains
             ^String (get-in response [:headers "content-type"])
             "text/plain")))))

  (testing "request translation"
    (with-server echo-handler {:port 4349}
      (let [response (http/get "http://localhost:4349/foo/bar/baz?surname=jones&age=123" {:body "hello"})]
        (is (= (:status response) 200))
        (is (= (:body response) "hello"))
        (let [request-map (clojure.edn/read-string
                           (get-in response [:headers "request-map"]))]
          (is (= (:query-string request-map) "surname=jones&age=123"))
          (is (= (:uri request-map) "/foo/bar/baz"))
          ;; This are no longer part of the Ring Spec, and are removed from the base request protocol
          ;(is (= (:content-length request-map) 5))
          ;(is (= (:character-encoding request-map) "UTF-8"))
          (is (= (:request-method request-map) :get))
          ;(is (= (:content-type request-map) "text/plain; charset=UTF-8"))
          (is (= (:remote-addr request-map) "127.0.0.1"))
          (is (= (:scheme request-map) :http))
          (is (= (:server-name request-map) "localhost"))
          (is (= (:server-port request-map) 4349))
          (is (= (:ssl-client-cert request-map) nil)))))))

