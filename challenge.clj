;; ---------------------
;; 1. Extend the graph definition to include a weight between graph edges
;; ---------------------
(def G 
  {:1 [[:2 1] [:3 2]]
   :2 [:4 4]
   :3 [:4 2]
   :4 []})

(println "\n1 - Graph Definition:\n  " G)


;; ---------------------
;; 2. Write an algorithm to randomly generate a simple directed graph using your answer from #1
;; ---------------------
(defn make-graph [n s]
  (when (or (< s (dec n)) (> s (* n (dec n))))
    (throw (IllegalArgumentException. "S must be between N-1 and N*(N-1)")))

  ;; Create vertices as keywords, ensuring :1 is first
  (let [vertices (concat [:1] (map keyword (map str (range 2 (inc n)))))
        edges (atom (set []))] ; Use an atom to store edges

    ;; Connect vertices in a chain to ensure initial connectivity
    (dotimes [i (dec n)]
      (let [from (nth vertices i)
            to (nth vertices (inc i))
            weight (rand-int 10)]
        (swap! edges conj [from to weight])))

    ;; Generate additional random edges until reaching the required number of edges
    (while (< (count @edges) s)
      (let [from (rand-nth vertices)
            to (rand-nth (remove #{from} vertices)) ;; Prevent self-loops
            weight (rand-int 10)]
        (when (and (not (contains? @edges [from to weight]))) ;; Prevent duplicates
          (swap! edges conj [from to weight]))))

    ;; Create the graph structure as a sorted map and include all vertices
    (let [graph (reduce (fn [g [from to weight]]
                          (update g from (fnil conj []) [to weight]))
                        (into (sorted-map) (map (fn [v] [v []]) vertices))  ;; Ensure all vertices are included
                        @edges)] ;; Build sorted map from edges
      graph)))



(def random-graph (make-graph 10 10)) ;; Generates a graph with 10 vertices and 10 edges
(println "\n2 - Generated Graph:\n  " random-graph)


;; ---------------------
;; 3. Write an implementation of Dijkstra's algorithm that traverses your graph and outputs the shortest path between any 2 randomly selected vertices.
;; ---------------------
(defn shortest-path [graph start end]
  (let [distances (atom (into {} (map (fn [v] [v Double/POSITIVE_INFINITY]) (keys graph))))
        previous (atom {})
        queue (atom (sorted-set-by (fn [[_ d1] [_ d2]] (compare d1 d2)) [start 0]))]

    ;; Set the distance to the start vertex to 0
    (swap! distances assoc start 0)

    (while (not (empty? @queue))
      (let [[current current-distance] (first @queue)]
        (do
          (swap! queue disj (first @queue)) ;; Remove current vertex from queue
          (doseq [[neighbor weight] (graph current)]
            (let [new-distance (+ current-distance weight)]
              (when (or (not (contains? @distances neighbor))
                        (< new-distance (@distances neighbor)))
                (swap! distances assoc neighbor new-distance)
                (swap! previous assoc neighbor current)
                (swap! queue conj [neighbor new-distance])))))))

    ;; Reconstruct the shortest path
    (loop [current end
           path []]
      (if (nil? current)
        (reverse path)
        (recur (get @previous current) (conj path current))))))


(println "\n3 - Shortest Path:\n  " (shortest-path random-graph (first (keys random-graph)) (last (keys random-graph))))


;; ---------------------
;; 4. Write a suite of functions to calculate distance properties for your graph.
;; ---------------------
;; Function to calculate the total weight of a path
(defn total-path-weight [graph path]
  (if (or (empty? path) (< (count path) 2))
    0 ;; If there is no path or if the path has less than 2 vertices, the weight is 0
    (let [weights (map (fn [[from to]]
                         ;; Find the weight for the edge from 'from' to 'to'
                         (let [edges (graph from)] ;; Get the edges for the current vertex
                           (some (fn [[neighbor weight]]
                                   (when (= neighbor to) weight)) 
                                 edges))) ;; Find the weight in the edges
                       (partition 2 1 path))] ;; Create pairs of (from, to) for adjacent vertices in the path
      (reduce + weights)))) ;; Sum the weights

;; Function to calculate the eccentricity of a vertex
(defn eccentricity [graph vertex]
  (let [all-vertices (keys graph)
        distances (map (fn [v]
                         (let [path (shortest-path graph vertex v)] ;; Get the shortest path
                           (total-path-weight graph path))) ;; Calculate the total weight for that path
                       all-vertices)]
    ;; Find the maximum distance to any reachable vertex
    (apply max distances))) ;; Return the maximum weight

;; Function to calculate the radius of the graph
(defn radius [graph]
  (apply min (map #(eccentricity graph %) (keys graph))))

;; Function to calculate the diameter of the graph
(defn diameter [graph]
  (apply max (map #(eccentricity graph %) (keys graph))))

(println "\n4 - Distance Properties:\n"  
         "  Eccentricity:" (eccentricity random-graph (first (keys random-graph)))
         "  Radius:" (radius random-graph)
         "  Diameter:" (diameter random-graph))


;; Extra: 

;; Debugging helper function to generate a Graphviz Dot representation of the graph
(defn graph-to-dot [graph]
  (let [edges (for [[from to-list] graph] ;; Iterate over each vertex and its edges
                (for [[to weight] to-list] ;; Iterate over each edge
                  (str (name from) " -> " (name to) " [label=\"" weight "\"];")))] ;; Create edge strings
    (str "  digraph G {\n     " ;; Start of the graph
         (apply str (interpose "\n     " (apply concat edges))) ;; Join edges with new lines
         "\n   }"))) ;; Closing the graph


;; Output the Graphviz format
(println "\n*Extra* - Graphviz output\n" (graph-to-dot random-graph))