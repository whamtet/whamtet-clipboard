(ns whamtet-clipboard.html
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]))

(require '[hiccup.page :as hiccup])
(require '[ring.util.response :as response])
(import java.io.File)
(require '[clojure.java.io :as io])
(import java.util.zip.ZipOutputStream)
(import java.io.FileOutputStream)
(import java.util.zip.ZipEntry)

(def store (File. "tmp"))
(.mkdirs store)
;(use 'clojure.pprint)
;(defn pr-response [x] (-> x pprint with-out-str response/response))

(defroutes routes
  (ANY "/upload" [poos ff]
       (let [
             poos (if (map? poos) [poos] poos)
             ff (if (map? ff) [ff] ff)
             files-selected? #(some (fn [{size :size}] (and size (not (zero? size)))) %)
             relavent-files (some #(if (files-selected? %) %) [poos ff])
             first-fn (-> relavent-files first :filename)
             ]
         (if first-fn
           (let [
                 zip-fn (if (.contains first-fn "/")
                          (first (.split first-fn "/"))
                          (first (.split first-fn "\\.")))
                 zip-fn (format "tmp/%s.zip" zip-fn)
                 ]
             (with-open [zos (-> zip-fn FileOutputStream. ZipOutputStream.)]
               (doseq [{:keys [tempfile filename]} relavent-files]
                 (.putNextEntry zos (ZipEntry. filename))
                 (io/copy tempfile zos)
                 (.closeEntry zos)))))
         (response/redirect "/")))
  (GET "/" []
       (let [
             ]
         (hiccup/html5
          [:body
           [:form {:action "/upload" :method "POST" :enctype "multipart/form-data"}
            "Directory Uploader" [:br]
            [:input {:type "file" :name "poos" :webkitdirectory "" :directory ""}][:br][:br]
            "File Upload" [:br]
            [:input {:name "ff" :type "file" :multiple true}][:br][:br]
            [:input {:type "Submit"}]][:br][:br]
           (for [f (.listFiles store)
                 :when (.endsWith (.getName f) ".zip")
                 ]
             [:div
              [:a {:href (.getName f)} (.getName f)]])
           ]))))
