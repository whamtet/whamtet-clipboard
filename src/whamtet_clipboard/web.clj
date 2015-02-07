(ns whamtet-clipboard.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY routes]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [cemerick.drawbridge :as drawbridge]
            [ring.util.response :as response]
            [whamtet-clipboard.html :as html]
            [environ.core :refer [env]]))

(defn- authenticated? [user pass]
  ;; TODO: heroku config:add REPL_USER=[...] REPL_PASSWORD=[...]
  (= [user pass] ["matthew" "friendster"]))

(defroutes app
  (route/resources "/" :root "../tmp")
  (ANY "*" []
       (route/not-found (slurp (io/resource "404.html")))))

(def app2 (routes #'html/routes
                  app))

(defonce store (cookie/cookie-store {:key (env :session-secret)}))

(defn wrap-app [app]
  ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
    (-> app
        (basic/wrap-basic-authentication authenticated?)
        trace/wrap-stacktrace
        (site {:session {:store store}})))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (wrap-app #'app2) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
