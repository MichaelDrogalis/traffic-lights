(ns traffic-lights.transform
  (:require [traffic-lights.queue :as q]
            [traffic-lights.index :as i]
            [traffic-lights.util :refer [maph]]))

(defn transform-ingress-lanes [old-i-lanes old-e-lanes safety-fn]
  (->> old-i-lanes
       (maph q/ch->lane)
       (maph q/mark-ripe)
       (maph q/advance-cars-in-lane)
       (maph #(q/harvest-ingress-lane % i/directions-index old-e-lanes safety-fn))))

(defn transform-egress-lanes [old-lanes]
  (->> old-lanes
       (maph q/ch->lane)
       (maph q/mark-ripe)
       (maph q/advance-cars-in-lane)
       (maph #(q/harvest-egress-lane % i/directions-index old-lanes))))

(defn transform-lights [old-lights]
  (->> old-lights
       (maph q/next-light-state)))
