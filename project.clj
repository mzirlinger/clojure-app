(defproject clojure-app "0.1.0-SNAPSHOT"
  :description "My clojure app"
  :url "localhost:3000"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]        ; logging framework
                 [amalloy/ring-buffer "1.2.1"
                  :exclusions [org.clojure/clojure
                               org.clojure/clojurescript]]  ; fixed length queue implementation, used in log buffering
                 [amalloy/ring-gzip-middleware "0.1.3"]     ; Ring middleware to GZIP responses if client can handle it
                 [compojure "1.1.8"]                        ; HTTP Routing library built on Ring
                 [honeysql "0.9.1"]                         ; Transform Clojure data structures to SQL
                 [log4j/log4j "1.2.17"]                     ; logging framework
                 [medley "1.0.0"]                           ; lightweight lib of useful functions
                 [migratus/migratus "1.0.0"]                ; database migrations
                 [postgresql "9.3-1102.jdbc41"]             ; Postgres driver
                 [ring/ring-core "1.6.0"]
                 [ring/ring-jetty-adapter "1.6.0"]          ; Ring adapter using Jetty webserver (used to run a Ring server for unit tests)
                 [ring/ring-json "0.4.0"]                   ; Ring middleware for reading/writing JSON automatically]
                 [toucan "1.1.0"                            ; Model layer, hydration, and DB utilities
                  :exclusions [honeysql]]
                 ]
  :plugins [[lein-environ "1.1.0"]
            [lein-ring "0.11.0"
             :exclusions [org.clojure/clojure]]]
  :main ^:skip-aot clojure-app.core
  :ring {:handler      clojure-app.core/app
         :auto-reload  true
         :auto-refresh true}
  :target-path "target/%s"
  :profiles {:dev                 {:dependencies [[expectations "2.2.0-beta2"] ; unit tests
                                                  [ring/ring-mock "0.3.0"]] ; Library to create mock Ring requests for unit tests
                                   :plugins      [[docstring-checker "1.0.2"] ; Check that all public vars have docstrings. Run with 'lein docstring-checker'
                                                  [jonase/eastwood "0.2.3"
                                                   :exclusions [org.clojure/clojure]] ; Linting
                                                  [lein-bikeshed "0.4.1"] ; Linting
                                                  [lein-expectations "0.0.8"] ; run unit tests with 'lein expectations'
                                                  [lein-instant-cheatsheet "2.2.1" ; use awesome instant cheatsheet created by yours truly w/ 'lein instant-cheatsheet'
                                                   :exclusions [org.clojure/clojure
                                                                org.clojure/tools.namespace]]]
                                   :env          {:app-run-mode "dev"}
                                   :jvm-opts     ["-Dlogfile.path=target/log"
                                                  "-Xms1024m" ; give JVM a decent heap size to start with
                                                  "-Xmx2048m"] ; hard limit of 2GB so we stop hitting the 4GB container limit on CircleCI
                                   :aot          [clojure-app.logger]} ; Log appender class needs to be compiled for log4j to use it
             :reflection-warnings {:global-vars {*warn-on-reflection* true}} ; run `lein check-reflection-warnings` to check for reflection warnings
             :expectations        {:injections     []
                                   :resource-paths ["test_resources"]
                                   :env            {:app-test-setting-1 "ABCDEFG"
                                                    :app-run-mode       "test"}
                                   :jvm-opts       ["-Duser.timezone=UTC"
                                                    "-Dapp.db.in.memory=true"
                                                    "-Dapp.jetty.join=false"
                                                    "-Dapp.jetty.port=3010"
                                                    "-Dapp.api.key=test-api-key"]}
             ;; build the uberjar with `lein uberjar`
             :uberjar             {:aot      :all
                                   :jvm-opts ["-Dclojure.compiler.elide-meta=[:doc :added :file :line]" ; strip out metadata for faster load / smaller uberjar size
                                              "-Dmanifold.disable-jvm8-primitives=true"]} ; disable Manifold Java 8 primitives (see https://github.com/ztellman/manifold#java-8-extensions)
             ;; Profile start time with `lein profile`
             :profile             {:jvm-opts ["-XX:+CITime" ; print time spent in JIT compiler
                                              "-XX:+PrintGC"]} ; print a message when garbage collection takes place
             })

