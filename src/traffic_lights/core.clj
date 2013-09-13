(ns traffic-lights.core
  (:require [clojure.pprint :refer [pprint]]
            [traffic-lights.transform :as t]
            [traffic-lights.boot :as b]
            [traffic-lights.protocols :as p]
            [traffic-lights.rules :as r]
            [traffic-lights.queue :as q]
            [traffic-lights.util :refer [maph index-by-lane-id]]))

(def schema
  (read-string (slurp (clojure.java.io/resource "intersection-schema.edn"))))

(def connections
  (read-string (slurp (clojure.java.io/resource "connections-schema.edn"))))

(def drivers
  (read-string (slurp (clojure.java.io/resource "drivers.edn"))))

(def directions
  (read-string (slurp (clojure.java.io/resource "directions.edn"))))

(def spec-source (concat schema connections drivers directions))

(def storage (p/memory-storage spec-source))

(def safety-fn (partial r/safe-to-go? storage))

(def lights (into {} (map (partial b/boot-light storage) (p/intersections storage))))

(def ingress-lanes 
  (->> storage
       (p/ingress-lanes)
       (map index-by-lane-id)
       (into {})
       (maph b/boot-lane)))

(def egress-lanes
  (->> storage
       (p/egress-lanes)
       (map index-by-lane-id)
       (into {})
       (maph b/boot-lane)))

(defn log! [idx]
  (pprint (maph :state idx)))

(defn genesis! [o-lights o-ilanes o-elanes safety-f]
  (log! o-lights)
  (log! o-ilanes)
  (log! o-elanes)
  (let [n-lights (t/transform-lights o-lights)
        n-elanes (t/transform-egress-lanes o-elanes)
        n-ilanes (t/transform-ingress-lanes o-ilanes o-elanes (partial safety-fn o-ilanes o-lights))]
    (Thread/sleep 1000)
    (recur n-lights n-ilanes n-elanes safety-f)))

;(q/put-into-ch (:channel (second (second ingress-lanes))) {:id "Mike" :len 3 :buf 0})

;(genesis! lights ingress-lanes egress-lanes safety-fn)


