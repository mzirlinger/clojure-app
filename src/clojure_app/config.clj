(ns clojure-app.config
  (:require (clojure.java [io :as io]
                          [shell :as shell])
            [clojure.string :as s])
  (:import clojure.lang.Keyword))


(def ^:private app-defaults
  "Global application defaults"
  {:app-db-logging       "true"
   ;; Jetty Settings. Full list of options is available here: https://github.com/ring-clojure/ring/blob/master/ring-jetty-adapter/src/ring/adapter/jetty.clj
   :app-jetty-port       "3000"
   :app-jetty-join       "true"
   ;; other application settings
   :app-session-age      "20160"                            ; session length in minutes (14 days)
   :app-qp-cache-backend "db"})


(defn config-str
  "Retrieve value for a single configuration key.  Accepts either a keyword or a string.

   We resolve properties from these places:

   1.  environment variables (ex: MB_DB_TYPE -> :mb-db-type)
   2.  jvm options (ex: -Dmb.db.type -> :mb-db-type)
   3.  hard coded `app-defaults`"
  [k]
  (let [k (keyword k)]
    ;(or (k environ/env) (k app-defaults))
    ))


;; These are convenience functions for accessing config values that ensures a specific return type
(defn ^Integer config-int "Fetch a configuration key and parse it as an integer." [k] (some-> k config-str Integer/parseInt))
(defn ^Boolean config-bool "Fetch a configuration key and parse it as a boolean." [k] (some-> k config-str Boolean/parseBoolean))
(defn ^Keyword config-kw "Fetch a configuration key and parse it as a keyword." [k] (some-> k config-str keyword))

(def ^:const ^Boolean is-dev? "Are we running in `dev` mode (i.e. in a REPL or via `lein ring server`)?" (= :dev (config-kw :app-run-mode)))
(def ^:const ^Boolean is-prod? "Are we running in `prod` mode (i.e. from a JAR)?" (= :prod (config-kw :app-run-mode)))
(def ^:const ^Boolean is-test? "Are we running in `test` mode (i.e. via `lein test`)?" (= :test (config-kw :app-run-mode)))

(defn- version-info-from-shell-script []
  (try
    (let [[tag hash branch date] (-> (shell/sh "./bin/version") :out s/trim (s/split #" "))]
      {:tag tag, :hash hash, :branch branch, :date date})
    ;; if ./bin/version fails (e.g., if we are developing on Windows) just return something so the whole thing doesn't barf
    (catch Throwable _
      {:tag "?", :hash "?", :branch "?", :date "?"})))

(defn- version-info-from-properties-file []
  (when-let [props-file (io/resource "version.properties")]
    (with-open [reader (io/reader props-file)]
      (let [props (java.util.Properties.)]
        (.load props reader)
        (into {} (for [[k v] props]
                   [(keyword k) v]))))))
