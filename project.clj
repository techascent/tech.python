(defproject techascent/tech.python "0.1-SNAPSHOT"
  :description "Techascent bindings to the python ecosystem"
  :url "https://github.com/techascent/tech.python"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1-beta2"]
                 [techascent/tech.datatype "4.0-alpha31"]
                 [black.ninia/jep "3.8.2"]]
  :repl-options {:init-ns tech.python})
