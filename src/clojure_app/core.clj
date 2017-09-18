(ns clojure-app.core
  (:gen-class)
  (:require [clojure
             [pprint :as pprint]]
            [clojure-app.config :as config]
            [clojure.tools.logging :as log]
            [compojure.core :refer :all]
            [medley.core :as m]
            [ring.adapter.jetty :as ring-jetty]
            [ring.middleware
             [cookies :refer [wrap-cookies]]
             [gzip :refer [wrap-gzip]]
             [json :refer [wrap-json-body wrap-json-response]]
             [keyword-params :refer [wrap-keyword-params]]
             [params :refer [wrap-params]]
             [session :refer [wrap-session]]])
  (:import org.eclipse.jetty.server.Server))

(defroutes app-routes
           (GET "/" [] "Hello World"))

(def ^:private app
  "The primary entry point to the Ring HTTP server."
  (-> #'app-routes                                          ; the #' is to allow tests to redefine endpoints
      (wrap-json-body                                       ; extracts json POST body and makes it available on request
        {:keywords? true})
      wrap-json-response                                    ; middleware to automatically serialize suitable objects as JSON in responses
      wrap-keyword-params                                   ; converts string keys in :params to keyword keys
      wrap-params                                           ; parses GET and POST params as :query-params/:form-params and both as :params
      wrap-cookies                                          ; Parses cookies in the request map and assocs as :cookies
      wrap-session))                                        ; reads in current HTTP session and sets :session/key


;;; ## ---------------------------------------- Jetty (Web) Server ----------------------------------------


(def ^:private jetty-instance
  (atom nil))

(defn start-jetty!
  "Start the embedded Jetty web server."
  []
  (when-not @jetty-instance
    (let [jetty-ssl-config (m/filter-vals identity {:ssl-port       (config/config-int :mb-jetty-ssl-port)
                                                    :keystore       (config/config-str :mb-jetty-ssl-keystore)
                                                    :key-password   (config/config-str :mb-jetty-ssl-keystore-password)
                                                    :truststore     (config/config-str :mb-jetty-ssl-truststore)
                                                    :trust-password (config/config-str :mb-jetty-ssl-truststore-password)})
          jetty-config (cond-> (m/filter-vals identity {:port          (config/config-int :mb-jetty-port)
                                                        :host          (config/config-str :mb-jetty-host)
                                                        :max-threads   (config/config-int :mb-jetty-maxthreads)
                                                        :min-threads   (config/config-int :mb-jetty-minthreads)
                                                        :max-queued    (config/config-int :mb-jetty-maxqueued)
                                                        :max-idle-time (config/config-int :mb-jetty-maxidletime)})
                               (config/config-str :mb-jetty-daemon) (assoc :daemon? (config/config-bool :mb-jetty-daemon))
                               (config/config-str :mb-jetty-ssl) (-> (assoc :ssl? true)
                                                                     (merge jetty-ssl-config)))]
      (log/info "Launching Embedded Jetty Webserver with config:\n" (with-out-str (pprint/pprint (m/filter-keys #(not (re-matches #".*password.*" (str %)))
                                                                                                                jetty-config))))
      ;; NOTE: we always start jetty w/ join=false so we can start the server first then do init in the background
      (->> (ring-jetty/run-jetty app (assoc jetty-config :join? false))
           (reset! jetty-instance)))))

(defn stop-jetty!
  "Stop the embedded Jetty web server."
  []
  (when @jetty-instance
    (log/info "Shutting Down Embedded Jetty Webserver")
    (.stop ^Server @jetty-instance)
    (reset! jetty-instance nil)))

(defn -main
  [& args]
  (log/info "starting the application")
  (log/info "Starting Metabase in STANDALONE mode")
  (try
    ;; launch embedded webserver async
    (start-jetty!)
    ;; Ok, now block forever while Jetty does its thing
    (when (config/config-bool :app-jetty-join)
      (.join ^Server @jetty-instance))
    (catch Throwable e
      (.printStackTrace e)
      (log/error "appa Initialization FAILED: " (.getMessage e))
      (System/exit 1))))

