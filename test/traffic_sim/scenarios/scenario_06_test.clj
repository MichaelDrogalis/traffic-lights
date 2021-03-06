(ns traffic-sim.scenarios.scenario-06-test
  "Driving one car up south street taking a right on red, yielding
   to traffic from the west."
  (:require [midje.sweet :refer :all]
            [traffic-sim.boot :as b]
            [traffic-sim.protocols :as p]
            [traffic-sim.rules :as r]
            [traffic-sim.queue :as q]
            [traffic-sim.util :as u]
            [traffic-sim.succession :refer :all]))

(def schema
  '[{:light-face/ident :standard
     :light-face/halt [:red]
     :light-face/proceed [:yellow :green]
     :light-face/init [:red]}
    
    {:schedule/ident :intx-schedule
     :schedule/substitute {?x :standard ?y :standard}
     :schedule/sequence [{:state-diff {?x [:green]} :ticks 1}]}
    
    {:rule/ident :straight
     :src ?origini
     :dst ?straighte
     :light [:green :yellow]}

    {:rule/ident :right-on-red
     :src ?origini
     :dst ?righte
     :yield [[?lefti ?righte]]
     :light [:red]}
    
    {:lane.rules/ident :no-turns
     :lane.rules/vars [?origini ?straighte]}

    {:lane.rules/ident :right-only
     :lane.rules/vars [?origini ?righte ?lefti]}
    
    {:lane.rules/of :no-turns
     :lane.rules/register :straight
     :lane.rules/substitute {?origini ?origini ?straighte ?straighte}}

    {:lane.rules/of :right-only
     :lane.rules/register :right-on-red
     :lane.rules/substitute {?origini ?origini ?righte ?righte ?lefti ?lefti}}

    {:intersection/ident ["Maple Street" "Leaf Street"]
     :intersection.install/schedule :intx-schedule}
    
    {:intersection/of ["Maple Street" "Leaf Street"]
     :street/name "Maple Street"
     :street/tag "south"
     :lane/name "in"
     :street.lane.install/ident ?a
     :street.lane.install/rules :right-only
     :street.lane.install/type :ingress
     :street.lane.install/length 5
     :street.lane.install/speed-limit 1
     :street.lane.install/light ?y
     :street.lane.install/substitute {?origini ?a
                                      ?righte ?B
                                      ?lefti ?c}}

    {:intersection/of ["Maple Street" "Leaf Street"]
     :street/name "Leaf Street"
     :street/tag "west"
     :lane/name "in"
     :street.lane.install/ident ?c
     :street.lane.install/rules :no-turns
     :street.lane.install/type :ingress
     :street.lane.install/length 5
     :street.lane.install/speed-limit 1
     :street.lane.install/light ?x
     :street.lane.install/substitute {?origini ?c
                                      ?straighte ?B}}
    
    {:intersection/of ["Maple Street" "Leaf Street"]
     :street/name "Leaf Street"
     :street/tag "east"
     :lane/name "out"
     :street.lane.install/ident ?B
     :street.lane.install/type :egress
     :street.lane.install/length 5
     :street.lane.install/speed-limit 1}])

(def storage (p/memory-storage schema))

(def safety-fn (r/safe-to-go? storage))

(def dir-fn
  (constantly
   {:intersection/of ["Maple Street" "Leaf Street"]
    :street/name "Leaf Street"
    :street/tag "east"
    :lane/name "out"}))

(def t-fn (transform-world-fn dir-fn dir-fn safety-fn))

(def lights (into {} (map (partial b/boot-light storage) (p/intersections storage))))

(def ingress-lanes (b/ingress-lanes storage))

(def egress-lanes (b/egress-lanes storage))

(def initial-world {:lights lights :ingress ingress-lanes :egress egress-lanes})

(def south-in
  {:intersection/of ["Maple Street" "Leaf Street"]
   :street/name "Maple Street"
   :street/tag "south"
   :lane/name "in"})

(def west-in
  {:intersection/of ["Maple Street" "Leaf Street"]
   :street/name "Leaf Street"
   :street/tag "west"
   :lane/name "in"})

(def east-out
  {:intersection/of ["Maple Street" "Leaf Street"]
   :street/name "Leaf Street"
   :street/tag "east"
   :lane/name "out"})

(q/put-into-ch (:channel (get ingress-lanes south-in)) {:id "Mike" :len 1 :buf 0})

(q/put-into-ch (:channel (get ingress-lanes west-in)) {:id "Kristen" :len 1 :buf 0})

(def iterations
  (reduce (fn [world _] (conj world (t-fn (last world)))) [initial-world] (range 20)))

(def ingress-south-iterations
  (map (comp (partial u/find-lane south-in) vals) (map :ingress iterations)))

(def ingress-west-iterations
  (map (comp (partial u/find-lane west-in) vals) (map :ingress iterations)))

(def egress-east-iterations
  (map (comp (partial u/find-lane east-out) vals) (map :egress iterations)))

(def light-iterations
  (map (comp :state first vals) (map :lights iterations)))

(fact (:state (nth ingress-south-iterations 1))
      => [{:id "Mike" :len 1 :buf 0 :front 4 :dst east-out}])

(fact (:state (nth ingress-west-iterations 1))
      => [{:id "Kristen" :len 1 :buf 0 :front 4 :dst east-out}])

(fact (:state (nth ingress-south-iterations 5))
      => [{:id "Mike" :len 1 :buf 0 :front 0 :dst east-out :ripe? false}])

(fact (:state (nth ingress-west-iterations 5))
      => [{:id "Kristen" :len 1 :buf 0 :front 0 :dst east-out :ripe? false}])

(fact (:state (nth ingress-west-iterations 6))
      => [])

(fact (:state (nth egress-east-iterations 6))
      => [{:id "Kristen" :len 1 :buf 0 :front 4 :dst east-out}])

(fact (:state (nth ingress-south-iterations 6))
      => [{:id "Mike" :len 1 :buf 0 :front 0 :dst east-out :ripe? true}])

(fact (:state (nth ingress-south-iterations 7))
      => [])

(fact (:state (nth egress-east-iterations 7))
      => [{:id "Kristen" :len 1 :buf 0 :front 3 :dst east-out :ripe? false}
          {:id "Mike" :len 1 :buf 0 :front 4 :dst east-out}])

