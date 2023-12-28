(ns demo.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [demo.events :as events]
   [demo.routes :as routes]
   [demo.views :as views]
   [demo.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init []
  (routes/start!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (re-frame/dispatch [:send-usage!])
  (mount-root))
