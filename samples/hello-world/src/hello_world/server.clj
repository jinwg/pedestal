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

(ns hello-world.server
  (:require [io.pedestal.http :as server]
            [hello-world.service :as service]))

(defonce runnable-service (server/create-server service/service))

(defn -main
  [& args]
  (println "\nCreating your server...")
  (server/start runnable-service))

