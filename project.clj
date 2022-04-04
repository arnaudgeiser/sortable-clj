(defproject sortable-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [org.postgresql/postgresql "42.3.1"]
                 [compojure "1.6.2"]
                 [ring "1.9.4"]
                 [ring/ring-json "0.5.1"]
                 [com.github.seancorfield/next.jdbc "1.2.753"]]
  :main sortable-clj.core
  :repl-options {:init-ns sortable-clj.core})
