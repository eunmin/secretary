(ns secretary.test.codec
  (:require
   [cemerick.cljs.test :as t]
   [secretary.codec :as codec])
  (:require-macros
   [cemerick.cljs.test :refer [deftest testing is are]]))

(deftest query-params-test
  (testing "encodes query params"
    (let [params {:id "kevin" :food "bacon"}
          encoded (codec/encode-query-params params)]
      (is (= (codec/decode-query-params encoded)
             params)))

    (are [x y] (= (codec/encode-query-params x) y)
      {:x [1 2]} "x[]=1&x[]=2"
      {:a [{:b 1} {:b 2}]} "a[0][b]=1&a[1][b]=2"
      {:a [{:b [1 2]} {:b [3 4]}]} "a[0][b][]=1&a[0][b][]=2&a[1][b][]=3&a[1][b][]=4"))

  (testing "decodes query params"
    (let [query-string "id=kevin&food=bacong"
          decoded (codec/decode-query-params query-string)
          encoded (codec/encode-query-params decoded)]
      (is (re-find #"id=kevin" query-string))
      (is (re-find #"food=bacon" query-string)))

    (are [x y] (= (codec/decode-query-params x) y)
      "x[]=1&x[]=2" {:x ["1" "2"]}
      "a[0][b]=1&a[1][b]=2" {:a [{:b "1"} {:b "2"}]}
      "a[0][b][]=1&a[0][b][]=2&a[1][b][]=3&a[1][b][]=4" {:a [{:b ["1" "2"]} {:b ["3" "4"]}]})))