(ns demo.events
  (:require
   [re-frame.core :as re-frame]
   [demo.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-fx
  ::navigate
  (fn [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))

(re-frame/reg-event-db
 :send-usage!
 (fn [{:keys [db]} _]
   (let [search (js/decodeURIComponent (.-search (.-location js/window)))
         from-where (second (re-find #"\?from=(.+)" search))
         endpoint (str "https://go.mazhangjing.com/track-smpg-" from-where)]
     (try
       (js/fetch endpoint {:method "GET" :mode "no-cors"})
       (catch js/Error _)))
   db))