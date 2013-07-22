(ns traffic-lights.core
  (:require [clojure.algo.generic.functor :refer [fmap]]
            [clojure.pprint :refer [pprint]]))

(def intersection-schema
  (read-string (slurp (clojure.java.io/resource "intersection-schema.edn"))))

(defn ensure-uniqueness [catalog]
  (fmap first catalog))

(defn build-catalog [schema kw]
  (ensure-uniqueness (group-by kw (filter #(contains? % kw) schema))))

(defn build-light-catalog [schema]
  (build-catalog schema :light-face/ident))

(defn build-schedule-catalog [schema]
  (build-catalog schema :schedule/ident))

(defn build-combo-catalog [schema]
  (build-catalog schema :light-combo/ident))

(def light-catalog (build-light-catalog intersection-schema))

(def schedule-catalog (build-schedule-catalog intersection-schema))

(def combo-catalog (build-combo-catalog intersection-schema))

(defn combo-resting-state [substitions light-catalog]
  (fmap #(:light-face/init (light-catalog %)) substitions))

(defn combo-schedule [schedule schedule-catalog]
  (:schedule/sequence (schedule-catalog schedule)))

;;; Begin experimentation.

(def combo (:standard-light-combo combo-catalog))

(def initial-lights (combo-resting-state (:light-combo/substitute combo) light-catalog))

(def schedule (combo-schedule (:light-combo/schedule combo) schedule-catalog))

(def traffic-light (agent initial-lights))
