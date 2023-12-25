(ns demo.merge
  (:require [clojure.string :as str]))

#_(def ice-map
  {"borderRouteDetection.json"   {:desc "运维监控 / 运维工具 / 网络诊断"
                                  :order 26}
   "deviceinspection.json"       {:desc "运维监控 / 运维工具 / 设备巡检"
                                  :order 25}
   "directconnect.json"          {:desc "虚拟网络 / 云专线"
                                  :order 11}
   "elastic.json"                {:desc "系统概况 / 概况"
                                  :order 1}
   "elk.json"                    {:desc "运维监控 / ELK监控"
                                  :order 16}
   "emergency.json"              {:desc "物理网络 / 交换机 / 特殊操作"
                                  :order 4}
   "epg.json"                    {:desc "虚拟网络 / EPG终端组"
                                  :order 10}
   "inspection.json"             {:desc "运维监控 / 运维工具 / 健康巡检"
                                  :order 23}
   "ipsla.json"                  {:desc "运维监控 / 运维工具 / IPSLA"
                                  :order 20}
   "l47-nocloud-rest.json"       {:desc "网络服务 / L47非云业务"
                                  :order 13}
   "l47service.json"             {:desc "网络服务 / L47设备和云业务"
                                  :order 14}
   "mtu.json"                    {:desc "运维监控 / 运维工具 / MTU检测"
                                  :order 21}
   "networks-map-view-rest.json" {:desc "系统概况 / 三网互视"
                                  :order 2}
   "ngoam.json"                  {:desc "运维监控 / 运维工具 / 路径检测"
                                  :order 24}
   "northSouthTraffic.json"      {:desc "运维监控 / 流量统计 / 南北向流量"
                                  :order 19}
   "overlaymapper.json"          {:desc "虚拟网络"
                                  :order 8}
   "physicalresource.json"       {:desc "物理网络 / 配置详情 / 交换机和服务器"
                                  :order 5}
   "portmirror.json"             {:desc "运维监控 / 运维工具 / SPAN探测"
                                  :order 22}
   "resourcepool.json"           {:desc "物理网络 / 资源池和资源管理"
                                  :order 6}
   "servermanage.json"           {:desc "物理网络 / 服务器管理"
                                  :order 7}
   "sfc.json"                    {:desc "网路服务 / 服务链"
                                  :order 15}
   "sys.json"                    {:desc "系统工具、日志和权限"
                                  :order 28}
   "telemetry.json"              {:desc "运维监控 / TELEMETRY监控"
                                  :order 17}
   "traffic-view.json"           {:desc "运维监控 / 流量统计 / VXLAN流量"
                                  :order 18}
   "troubleshooting.json"        {:desc "运维监控 / 运维工具 / 智能巡检"
                                  :order 27}
   "underlay.json"               {:desc "物理网络"
                                  :order 3}
   "vmware.json"                 {:desc "虚拟网络 / Vmware"
                                  :order 12}
   "vnnb.json"                   {:desc "虚拟网络 / 非云服务"
                                  :order 9}})

(defn make-unique! [from-data prefix file-pfx file-order]
  (let [{:keys [swagger info basePath tags schemes paths securityDefinitions definitions]} from-data
        concat-path (fn [kw]
                      (let [k (name kw)
                            a (str/ends-with? basePath "/")
                            b (str/starts-with? k "/")]
                        (cond (and a b)
                              (keyword (str basePath (subs k 1)))
                              (and (not a) (not b))
                              (keyword (str basePath "/" k))
                              :else
                              (keyword (str basePath k)))))
        trans-name (fn [old-name] (str/upper-case (str file-pfx old-name)))
        tags-map    (reduce (fn [agg {:keys [name]}] (assoc agg name (trans-name name))) {} tags)
        tags        (mapv
                     (fn [{:keys [name]}]
                       {:name (trans-name name)
                        :order file-order})
                     tags)
        paths       (reduce-kv (fn [m k v] (assoc m (concat-path k) v)) {} paths)
        paths       (reduce-kv
                     (fn [m k v] ;path methods
                       (assoc
                        m k
                        (reduce-kv
                         (fn [m k v] ;method definition 
                           (assoc
                            m k
                            (reduce-kv
                             (fn [m k v] ;parameter schema
                               (cond (and (= :parameters k)
                                          (vector? v))
                                     (assoc
                                      m k
                                      (mapv (fn [x] ;one parameter map
                                              (reduce-kv
                                               (fn [m k v] ;schema ref
                                                 (if (and (= :schema k)
                                                          (map? v)
                                                          (contains? v :$ref))
                                                   (assoc m k
                                                          (assoc v
                                                                 :$ref
                                                                 (let [ref (get v :$ref)]
                                                                   (if (and (string? ref)
                                                                            (re-find #"^#/definitions/.*" ref))
                                                                     (str "#/definitions/" prefix (subs ref 14))
                                                                     ref))))
                                                   (assoc m k v)))
                                               {}
                                               x))
                                            v))
                                     (and (= :responses k)
                                          (map? v))
                                       ;遍历所有的 XXX: map 中的 map，并当 key 为 schema 且
                                       ;additionalProperties 为 map 且包含 $ref 时进行替换
                                     (assoc
                                      m k
                                      (reduce-kv
                                       (fn [m k v] ;code response
                                         (if (and (map? v)
                                                  (contains? v :schema)
                                                  (map? (:schema v))
                                                  (contains? (:schema v) :additionalProperties)
                                                  (map? (:additionalProperties (:schema v)))
                                                  (contains? (:additionalProperties (:schema v)) :$ref))
                                           (assoc
                                            m k
                                            (assoc v
                                                   :schema
                                                   (assoc (:schema v)
                                                          :additionalProperties
                                                          (assoc (:additionalProperties (:schema v))
                                                                 :$ref
                                                                 (let [ref (get (:additionalProperties (:schema v)) :$ref)]
                                                                   (if (and (string? ref)
                                                                            (re-find #"^#/definitions/.*" ref))
                                                                     (str "#/definitions/" prefix (subs ref 14))
                                                                     ref))))))
                                           (assoc m k v)))
                                       {}
                                       v))
                                     (and (= :tags k)
                                          (vector? v))
                                     (assoc m k
                                            (mapv
                                             (fn [x] (if (contains? tags-map x) (get tags-map x) x))
                                             v))
                                     :else
                                     (assoc m k v)))
                             {}
                             v)))
                         {}
                         v)))
                     {}
                     paths)
        definitions (reduce-kv (fn [m k v]
                                 (assoc m (keyword (str prefix (name k))) v))
                               {}
                               definitions)
        ;如果 definitions 的 properties 中某个 key 的 value map 中包含 $ref，将其改为 #/definitions/ZZZ-xxx
        definitions (reduce-kv
                     (fn [m k v] ;className properties
                       (assoc m k
                              (if (map? v)
                                (reduce-kv
                                 (fn [m k v] ;type/properties...
                                   (if (and (= :properties k)
                                            (map? v))
                                     (assoc
                                      m k
                                      (reduce-kv
                                       (fn [m k v] ;propertyName property
                                         (cond (and (map? v)
                                                    (contains? v :$ref)) ;如果 property 中包含直接 $ref
                                               (assoc
                                                m k
                                                (assoc v
                                                       :$ref
                                                       (let [ref (get v :$ref)]
                                                         (if (and (string? ref)
                                                                  (re-find #"^#/definitions/.*" ref))
                                                           (str "#/definitions/" prefix (subs ref 14))
                                                           ref))))
                                               (and (map? v)
                                                    (contains? v :items)
                                                    (contains? (:items v) :$ref)) ;如果 property 中 items 中包含 $ref
                                               (assoc
                                                m k
                                                (assoc v
                                                       :items
                                                       (assoc (:items v)
                                                              :$ref
                                                              (let [ref (get (:items v) :$ref)]
                                                                (if (and (string? ref)
                                                                         (re-find #"^#/definitions/.*" ref))
                                                                  (str "#/definitions/" prefix (subs ref 14))
                                                                  ref)))))
                                               :else
                                               (assoc m k v)))
                                       {}
                                       v))
                                     (assoc m k v)))
                                 {}
                                 v)
                                v)))
                     {}
                     definitions)
        result      {:swagger             swagger
                     :info                info
                     :basePath            basePath
                     :tags                tags
                     :schemes             schemes
                     :paths               paths
                     :securityDefinitions securityDefinitions
                     :definitions         definitions}]
    result))

(defn random-prefix []
  (.substring (str (random-uuid)) 0 9))

(defn merge! [data {:keys [mapping document-title
                           document-desc
                           document-version]}]
  (let [datas       (mapv
                     (fn [[name content]]
                       (let [{:keys [desc order]} (get mapping (keyword name))
                             file-pfx (if desc (str desc " / ") "")
                             file-order (or order 0)]
                         (make-unique! content (random-prefix) file-pfx file-order)))
                     data)
        merged-data (reduce
                     (fn [agg m1]
                       (let [{swagger-1             :swagger
                              info-1                :info
                              basePath-1            :basePath
                              schemes-1             :schemes
                              securityDefinitions-1 :securityDefinitions
                              tags-1                :tags
                              paths-1               :paths
                              definitions-1         :definitions} agg
                             {basePath-2            :basePath
                              swagger-2             :swagger
                              info-2                :info
                              tags-2                :tags
                              schemes-2             :schemes
                              paths-2               :paths
                              securityDefinitions-2 :securityDefinitions
                              definitions-2         :definitions} m1]
                         {:swagger             swagger-2
                          :info                info-2
                          :basePath            "/"
                          :tags                (set (into (or tags-1 []) (or tags-2 [])))
                          :schemes             schemes-2
                          :paths               (merge paths-1 paths-2)
                          :securityDefinitions securityDefinitions-2
                          :definitions         (merge definitions-1 definitions-2)}))
                     {}
                     datas)
        merged-data (assoc
                     merged-data
                     :tags
                     (->> merged-data
                          :tags
                          (vec)
                          (sort-by :order)
                          (mapv (fn [x] (dissoc x :order))))
                     :info
                     (merge (:info merged-data)
                            (if-not (or (nil? document-version)
                                        (str/blank? document-version))
                              {:description (or document-desc "")
                               :title       document-title
                               :version     document-version}
                              {:description (or document-desc "")
                               :title       document-title})))]
    merged-data))