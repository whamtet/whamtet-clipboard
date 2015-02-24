(ns whamtet-clipboard.html
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]))

(require '[hiccup.page :as hiccup])
(require '[ring.util.response :as response])
(import java.io.File)
(require '[clojure.java.io :as io])
(import java.util.zip.ZipOutputStream)
(import java.io.FileOutputStream)
(import java.io.FileInputStream)
(import java.util.zip.ZipEntry)

(def store (File. "resources/public"))
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
                 zip-fn (format "resources/public/%s.zip" zip-fn)
                 ]
             (with-open [zos (-> zip-fn FileOutputStream. ZipOutputStream.)]
               (doseq [{:keys [tempfile filename]} relavent-files]
                 (.putNextEntry zos (ZipEntry. filename))
                 (io/copy tempfile zos)
                 (.closeEntry zos)))))
         (response/redirect "/")))
  (ANY "/upload-text" [text]
       (spit "resources/public/clipboard.txt" text)
       (response/redirect "/"))
  (GET "/download" [fname]
       (let [
             f (File. (str "resources/public/" fname))
             ]
         {:status 200
          :headers {"Content-Type" "application/zip"
                    "Content-Disposition" (format "attachment; filename=\"%s\"" fname)
;                    "Content-Length" (.length f)
                    }
          :body (FileInputStream. f)}))
  (GET "/delete" [fname]
       (-> (str "resources/public/" fname) File. .delete)
       (response/redirect "/"))
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
                 :when (or (= "clipboard.txt" (.getName f)) (.endsWith (.getName f) ".zip"))
                 ]
             [:div
              [:a {:href (format "/download?fname=%s" (.getName f))} (.getName f)]
              " "
              [:a {:href (format "/delete?fname=%s" (.getName f))} "X"]
              ])
           [:br]
           [:form {:action "/upload-text" :method "POST"}
            [:textarea {:name "text" :style "width:90%; height: 500px;"}][:br][:br]
            [:input {:type "Submit"}]
            ]
           ]))))
