(ns demo.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [demo.events :as events]
   [demo.routes :as routes]
   [demo.demo :as demo]
   [demo.subs :as subs]
   [demo.merge :as merge]
   [clojure.string :as str]))

(defn read-file
  [file callback]
  (let [reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [e]
            (callback (-> e .-originalTarget .-result))))
    (.readAsText reader file)))

(defn download-blob-as-file
  [blob filename]
  (let [a (js/document.createElement "a")
        url (js/URL.createObjectURL blob)]
    (.setAttribute a "href" url)
    (.setAttribute a "download" filename)
    (.click a)))

(defn merge-files!
  [files config gen-pdf?]
  (let [merged-file-contents 
        (merge/merge! files 
                      (update config
                              :mapping
                              #(-> % 
                                   (js/JSON.parse)
                                   (js->clj :keywordize-keys true))))
        merged-data (clj->js merged-file-contents
                             :keyword-fn #(subs (str %) 1))
        merged-json (js/JSON.stringify merged-data nil 2)]
    (if-not gen-pdf?
      (download-blob-as-file
       (js/Blob. [merged-json]
                 {:type "application/json"}) (or (:output-file-name config) "merge.json"))
      (let [element (.getElementById js/document "gen-pdf")]
        (.generatePdf element merged-data)))))

(defonce dropped-files (r/atom {}))

(defonce config (r/atom {:output-file-name "merge.json"
                         :document-title   "Swagger Document"
                         :document-desc    "Swagger Document of API"
                         :document-version "1.0.0"
                         :mapping          (js/JSON.stringify
                                            (clj->js
                                             {"borderRouteDetection.json" {:desc  "DevOps Monitoring / DevOps Tooling / Network Troubleshooting"
                                                                           :order 26}
                                              "deviceinspection.json"     {:desc  "DevOps Monitoring / DevOps Tooling / Device Inspection"
                                                                           :order 25}})
                                            nil 2)}))

(defn handle-upload-file [e]
  (let [files (.-files (.-target e))]
    (doseq [file files]
      (read-file file
                 (fn [contents]
                   (swap! dropped-files assoc (.-name file)
                          (-> (js/JSON.parse contents)
                              (js->clj :keywordize-keys true))))))))

(defn home-panel []
  [:<>
   [:nav.navbar.is-info
    [:div.container
     [:div.navbar-brand
      [:a.navbar-item 
       [:svg {:t       "1703483238104"
              :class   "icon"
              :viewBox "0 0 1024 1024"
              :version "1.1"
              :xmlns   "http://www.w3.org/2000/svg"
              :p-id    "4219"
              :height  "200"
              :width   "200"}
        [:path {:d    "M511.18 1018.06C227.739 1016.833 14.337 792.372 6.145 532.686-3.686 224.665 236.339 14.336 490.906 6.144c307.404-9.83 516.505 229.99 526.745 481.485 12.698 306.38-233.472 532.48-506.47 530.432z"
                :fill "#6D9900"
                :p-id "4220"}]
        [:path {:d    "M272.589 512c33.997 20.07 44.646 50.586 48.742 85.197 3.072 26.01 1.639 52.019 2.867 78.029 1.844 38.297 12.084 39.321 39.527 39.321 11.878 0 16.384 3.482 14.54 15.155-0.409 1.844 0 3.687 0 5.735 0 33.587 0 33.587-33.791 32.153-47.514-2.048-73.114-29.286-77.62-76.8-3.276-35.43 1.434-71.065-5.12-106.086-5.324-28.672-16.793-39.936-45.875-41.574-7.373-0.41-10.24-2.663-10.035-9.83 0.41-7.169 0-14.132 0-21.3 0.205-9.011-2.048-19.046 1.229-26.624 3.891-8.806 15.36-2.662 23.347-5.12 14.336-4.3 23.552-13.722 27.853-27.648 3.686-11.674 5.939-23.757 6.144-36.045 0.614-32.153-2.048-64.512 4.915-96.051 9.216-42.598 29.082-59.802 72.704-63.693 36.045-3.277 36.045-3.277 36.045 32.563 0 19.661 0 19.866-18.842 20.276-28.877 0.614-32.768 9.42-35.02 36.864-2.049 25.804 0 52.019-2.868 78.028-3.686 35.43-13.926 66.765-48.742 87.45z m478.617 0.205c-38.092-22.733-46.694-58.573-49.356-98.304-1.434-23.143-1.23-46.285-2.458-69.427-1.229-26.624-8.602-33.997-35.226-35.021-19.046-0.615-19.046-0.615-19.046-20.07v-2.868c0-30.925 0-30.925 31.744-30.105 48.947 1.433 74.752 27.238 79.462 75.776 2.663 27.443 1.434 54.886 2.458 82.124 0.41 13.312 2.867 26.215 6.963 38.708 6.554 19.456 16.999 27.443 37.683 27.648 11.06 0 15.36 3.072 14.336 14.54-1.024 12.288-0.614 24.576-0.204 36.864 0.204 7.783-2.458 10.445-10.445 10.855-28.263 1.229-40.755 13.721-45.67 42.189-4.097 23.142-2.663 46.284-2.868 69.222 0 16.998-1.024 33.997-4.71 50.586-9.421 41.37-29.901 58.777-72.09 62.259-36.864 3.072-35.84 2.867-37.273-33.178-0.615-15.36 3.276-19.865 19.046-19.66 27.648 0.204 34.406-7.988 35.635-35.84 1.229-30.72 0.41-61.44 4.915-91.956 5.12-31.13 17.613-57.139 47.104-74.342z"
                :fill "#FEFEFE"
                :p-id "4221"}]
        [:path {:d    "M635.7 545.997c-19.252 0.205-35.636-15.565-35.636-33.997 0-17.818 16.18-33.997 34.406-34.202 19.252-0.204 35.636 15.565 35.636 34.202 0 18.022-15.975 33.792-34.407 33.997z m-89.908-34.202c0 20.48-13.926 34.407-34.406 34.407-19.252 0-33.792-14.541-33.792-33.588 0-19.456 15.155-34.61 34.61-34.61 19.252 0 33.588 14.335 33.588 33.791zM389.12 545.997c-20.07 0-34.816-14.746-34.611-34.407 0.205-19.25 14.95-33.587 33.997-33.792 19.046 0 35.225 15.975 35.02 34.816-0.204 18.228-15.77 33.383-34.406 33.383z"
                :fill "#FEFEFE"
                :p-id "4222"}]]
       [:span.ml-2 "SMPG"]]]
     [:div.navbar-menu
      [:div.navbar-start
       [:a.navbar-item {:on-click #(re-frame/dispatch [::events/navigate :home])}
        "Home"]
       [:a.navbar-item {:on-click #(js/window.open "https://github.com/corkine" "_blank")}
        [:svg.mr-1 {:t       "1703483460437"
                    :class   "icon"
                    :viewBox "0 0 1024 1024"
                    :version "1.1"
                    :xmlns   "http://www.w3.org/2000/svg"
                    :p-id    "5226"
                    :width   200
                    :height  200}
         [:path {:d    "M850.346667 155.008a42.666667 42.666667 0 0 0-22.741334-23.509333c-8.704-3.754667-85.717333-33.322667-200.32 39.168H396.714667c-114.773333-72.618667-191.701333-42.922667-200.32-39.168a42.88 42.88 0 0 0-22.741334 23.466666c-26.197333 66.218667-18.048 136.448-7.850666 176.896C134.272 374.016 128 413.098667 128 469.333333c0 177.877333 127.104 227.882667 226.730667 246.272a189.568 189.568 0 0 0-13.013334 46.549334A44.373333 44.373333 0 0 0 341.333333 768v38.613333c-19.498667-4.138667-41.002667-11.946667-55.168-26.112C238.08 732.416 188.330667 682.666667 128 682.666667v85.333333c25.002667 0 65.365333 40.362667 97.834667 72.832 51.029333 51.029333 129.066667 55.253333 153.386666 55.253333 3.114667 0 5.376-0.085333 6.528-0.128A42.666667 42.666667 0 0 0 426.666667 853.333333v-82.090666c4.266667-24.746667 20.224-49.621333 27.946666-56.362667a42.666667 42.666667 0 0 0-23.125333-74.581333C293.333333 624.554667 213.333333 591.488 213.333333 469.333333c0-53.12 5.632-70.741333 31.573334-99.285333 11.008-12.117333 14.08-29.568 7.978666-44.8-4.821333-11.904-18.773333-65.450667-6.485333-117.546667 20.650667-1.578667 59.904 4.565333 113.706667 40.96C367.104 253.44 375.466667 256 384 256h256a42.666667 42.666667 0 0 0 23.936-7.338667c54.016-36.522667 92.970667-41.770667 113.664-41.130666 12.330667 52.224-1.578667 105.770667-6.4 117.674666a42.666667 42.666667 0 0 0 8.021333 44.928C805.077333 398.464 810.666667 416.085333 810.666667 469.333333c0 122.581333-79.957333 155.52-218.069334 170.922667a42.666667 42.666667 0 0 0-23.125333 74.709333c19.797333 17.066667 27.861333 32.469333 27.861333 53.034667v128h85.333334v-128c0-20.437333-3.925333-38.101333-9.770667-53.12C769.92 695.765333 896 643.712 896 469.333333c0-56.362667-6.272-95.530667-37.76-137.514666 10.197333-40.405333 18.261333-110.506667-7.893333-176.810667z"
                 :fill "#ffffff"
                 :p-id "5227"}]]
        "About"]]]]]
   [:section.hero.is-info
    [:div.hero-body
     [:div.container
      [:h1.title
       [:span "Swagger Merge and PDF Generator"]]
      [:h2.subtitle.pt-3
       [:span.is-size-6 "Concat multiple swagger specs into one, and generate PDF for it!"]
       [:br]
       [:span.is-size-6 "Simplified and Traditional Chinese Export Support"]
       [:br]
       [:span.is-size-6 "Powered by "
        [:a  {:style {:text-decoration "underline"}
              :on-click #(js/window.open "https://mrin9.github.io/RapiPdf/" "_blank")} "RapiPdf"]
        " and ClojureScript"]]]]]
   [:div.container.content.mt-5
    [:div.ml-5
     [:div.file.is-boxed.mt-3
      [:label.file-label
       [:input.file-input {:type      "file"
                           :accept    ".json"
                           :multiple  :multiple
                           :on-change handle-upload-file}]
       [:span.file-cta
        [:span.file-label "Read Swagger .json file(s)"]]]]
     [:div.mt-3
      (if (empty? @dropped-files)
        [:div.has-text-danger "No files uploaded"]
        [:div
         [:div
          (for [[k v] @dropped-files]
            ^{:key k}
            [:div.mb-2
             [:div
              [:strong k]]
             [:div
              [:pre (pr-str v)]]])]])]
     [:div.mt-5.mb-5
      [:h4 "Config Settings"]
      [:div.field.is-horizontal
       [:div.field-label.is-normal
        [:label.label "Output File Name"]]
       [:div.field-body
        [:div.field
         [:p.control
          [:input.input {:type          "text"
                         :on-change     (fn [e]
                                          (let [data (-> e .-target .-value)]
                                            (when-not (or (nil? data) (str/blank? data))
                                              (swap! config assoc :output-file-name data))))
                         :placeholder   "Output File Name"
                         :default-value (:output-file-name @config)}]]]]]
      [:div.field.is-horizontal
       [:div.field-label.is-normal
        [:label.label "Document Title"]]
       [:div.field-body
        [:div.field
         [:p.control
          [:input.input {:type          "text"
                         :on-change     (fn [e]
                                          (let [data (-> e .-target .-value)]
                                            (when-not (or (nil? data) (str/blank? data))
                                              (swap! config assoc :document-title data))))
                         :placeholder   "Document Title"
                         :default-value (:document-title @config)}]]]]]
      [:div.field.is-horizontal
       [:div.field-label.is-normal
        [:label.label "Document Desc"]]
       [:div.field-body
        [:div.field
         [:p.control
          [:input.input {:type          "text"
                         :on-change     (fn [e]
                                          (let [data (-> e .-target .-value)]
                                            (when-not (or (nil? data) (str/blank? data))
                                              (swap! config assoc :document-desc data))))
                         :placeholder   "Document Description"
                         :default-value (:document-desc @config)}]]]]]
      [:div.field.is-horizontal
       [:div.field-label.is-normal
        [:label.label "Document Version"]]
       [:div.field-body
        [:div.field
         [:p.control
          [:input.input {:type          "text"
                         :on-change     (fn [e]
                                          (let [data (-> e .-target .-value)]
                                            (when-not (or (nil? data) (str/blank? data))
                                              (swap! config assoc :document-version data))))
                         :placeholder   "Document Version"
                         :default-value (:document-version @config)}]]]]]
      [:div.field.is-horizontal
       [:div.field-label.is-normal
        [:label.label "File2TagName Map"]]
       [:div.field-body
        [:div.field
         [:p.control
          [:textarea.textarea
           {:placeholder   "Mapping FileName to TagName Prefix, and set it's order"
            :rows          10
            :on-change     (fn [e]
                             (let [data (-> e .-target .-value)]
                               (if (or (nil? data) (str/blank? data))
                                 (swap! config assoc :mapping {})
                                 (try
                                   (swap! config assoc :mapping (js/JSON.parse data))
                                   (catch js/Error _ (println "Invalid JSON format!"))))))
            :default-value (:mapping @config)}]]]]]
      [:div.field.is-horizontal
       [:div.field-label.is-normal]
       [:div.field-body
        [:div.field
         [:article.message.is-warning
          [:div.message-body
           "Add a prefix to and sort the tags of a specific .json file。e.g. if a.json sets desc to \"A\", then all the tags in its file become \"A / Tag\", and the tags will be sorted by the order field after the files are merged."]]]]]]
     [:div.mt-5.mb-5.is-flex.is-justify-content-flex-end
      (when (empty? @dropped-files)
        [:div
         [:button.button.is-info
          {:on-click #(merge-files! (demo/try-with-demo) @config true)}
          "Try with Demo Swagger Spec!"]])
      (when (not-empty @dropped-files)
        [:button.button.is-danger
         {:on-click #(reset! dropped-files {})}
         "Clean Read Files"])
      (when (not-empty @dropped-files)
        [:button.button.is-info.ml-2
         {:on-click (fn []
                      (println @config)
                      (merge-files! @dropped-files @config false))}
         "Download Merged .json File"])
      (when (not-empty @dropped-files)
        [:button.button.is-info.ml-2
         {:on-click (fn []
                      (println @config)
                      (merge-files! @dropped-files @config true))}
         "Merge and Generated PDF!"])]]]])

(defmethod routes/panels :home-panel [] [home-panel])

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:on-click #(re-frame/dispatch [::events/navigate :home])}
     "go to Home Page"]]])

(defmethod routes/panels :about-panel [] [about-panel])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    (routes/panels @active-panel)))
