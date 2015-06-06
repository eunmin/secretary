# secretary

클로저스크립트에서 사용하는 클라이언트 라우터

[ ![Codeship Status for gf3/secretary](https://codeship.io/projects/0b3ff6f0-ca42-0131-5e5a-5e2103a31145/status?branch=master)](https://codeship.io/projects/22531)


## Contents

- [설치](#installation)
- [가이드](#guide)
  * [기본적인 라우팅과 디스패치](#basic-routing-and-dispatch)
  * [라우트 매쳐](#route-matchers)
  * [Parameter destructuring](#parameter-destructuring)
  * [Query parameters](#query-parameters)
  * [Named routes](#named-routes)
- [Example with history](#example-with-googhistory)
- [Available protocols](#available-protocols)
- [Contributors](#contributors)
- [Committers](#committers)


## Installation

`project.clj` 파일 안에 `:dependencies` 벡터에 secretary를 추가한다:

```clojure
[secretary "1.2.3"]
```

현재 `SNAPSHOT` 버전은 아래와 같다:

```clojure
[secretary "1.2.4-SNAPSHOT"]
```

## 가이드

secretary를 사용하려면 secretary를 `:require` 해준다.

```clojure
(ns app.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]])
```


### 기본적인 라우팅과 액션 실행

Secretary는 두가지 목적을 위해 만들어졌다: 라우트 매쳐를 생성하고 알맞은 액션을 실행하는 것이다.
라우터 매쳐는 기본적인 매칭기능과 URI에서 파라메터 가져오는 기능, 그리고 함수에서 그 파라메터를 사용할 수 있도록 해준다.

`defroute`는 Secretary의 라우트 매쳐와 액션을 연결해주는 기본 매크로다.
`defroute` 매크로의 시그네쳐는 `[name? route destruct & body]`다.
`name?`은 [named routes](#named-routes)을 다룰때 설명할 것이기 때문에 지금은 무시하자.
이해를 돕기 위해 id 파라미터를 받은 users 라우트를 정의해보자.

```clojure
(defroute "/users/:id" {:as params}
  (js/console.log (str "User: " (:id params))))
```

여기서 `"/users/:id"`는 라우트 매쳐라고 부르고 있는 `route`다.
`{:as params}`는 라우트 매쳐의 매칭된 결과에서 파라메터 값을 디스트럭처링 하는 `destruct`다.
그리고 나머지 부분은 라우트 액션인 `body`다.

더 자세히 알아보기 전에 지금 만든 라우터를 디스패치해서 확인해보자.

```clojure
(secretary/dispatch! "/users/gf3")
```

별 문제가 없다면 페이지를 새로고침 하면 콘솔창에 `User: gf3`라고 나오는 것을 볼 수 있다.

#### 라우트 매쳐

기본적으로 라우트 메쳐는 문자열이나 정규식을 쓸 수 있다.
문자열을 사용하는 경우 [Sinatra][sinatra]나 [Ruby on Rails][rails]를 사용해 본 사람들은 익숙할 만한 형태를 가지고 있다.
특정 URI를 가지고 `secretary/dispatch!`를 부르면 라우트는 URI에 매칭되는 라우트를 찾고 그 라우트에 연결된 액션을 수행 한다.
라우트를 찾으면 URI에서 파라메터도 가져올 수 있다.
문자열 라우트의 경우에는 이 파라메터들은 맵 형식을 가지고 정규식 라우트인 경우에는 벡터로 표현된다.

위의 예에서 `"/users/gf3"` URI는 `"/users/:id"` 라우트에 매칭되고 `{:id "gf3}` 라는 파라메터 맵을 가진다.
아래는 다양한 라우트 매쳐와 URI와 매칭이 되었을 때의 파라메터 맵을 보여주고 있다.

라우드 메쳐          | URI              | 파라메터
---------------------|------------------|--------------------------
`"/:x/:y"`           | `"/foo/bar"`     | `{:x "foo" :y "bar"}`
`"/:x/:x"`           | `"/foo/bar"`     | `{:x ["foo" "bar"]}`
`"/files/*.:format"`  | `"/files/x.zip"` | `{:* "x" :format "zip"}`
`"*"`                | `"/any/thing"`   | `{:* "/any/thing"}`
`"/*/*"`             | `"/n/e/thing"`   | `{:* ["n" "e/thing"]}`
`"/*x/*y"`           | `"/n/e/thing"`   | `{:x "n" :y "e/thing"}`
`#"/[a-z]+/\d+"`     | `"/foo/123"`     | `["/foo/123"]`
`#"/([a-z]+)/(\d+)"` | `"/foo/123"`     | `["foo" "123"]`


#### Parameter destructuring

Now that we understand what happens during dispatch we can look at the
`destruct` argument of `defroute`. This part is literally sugar
around `let`. Basically whenever one of our route matches is
successful and extracts parameters this is where we destructure
them. Under the hood, for example with our users route, this looks
something like the following.

```clojure
(let [{:as params} {:id "gf3"}]
  ...)
```

Given this, it should be fairly easy to see that we could have have
written

```clojure
(defroute "/users/:id" {id :id}
  (js/console.log (str "User: " id)))
```

and seen the same result. With string route matchers we can go even
further and write

```clojure
(defroute "/users/:id" [id]
  (js/console.log (str "User: " id)))
```

which is essentially the same as saying `{:keys [id]}`.

For regular expression route matchers we can only use vectors for
destructuring since they only ever return vectors.

```clojure
(defroute #"/users/(\d+)" [id]
  (js/console.log (str "User: " id)))
```


#### Query parameters

If a URI contains a query string it will automatically be extracted to
`:query-params` for string route matchers and to the last element for
regular expression matchers.

```clojure
(defroute "/users/:id" [id query-params]
  (js/console.log (str "User: " id))
  (js/console.log (pr-str query-params)))

(defroute #"/users/(\d+)" [id {:keys [query-params]}]
  (js/console.log (str "User: " id))
  (js/console.log (pr-str query-params)))

;; In both instances...
(secretary/dispatch! "/users/10?action=delete")
;; ... will log
;; User: 10
;; "{:action \"delete\"}"
```


#### Named routes

While route matching and dispatch is by itself useful, it is often
necessary to have functions which take a map of parameters and return
a URI. By passing an optional name to `defroute` Secretary will
define this function for you.

```clojure
(defroute users-path "/users" []
  (js/console.log "Users path"))

(defroute user-path "/users/:id" [id]
  (js/console.log (str "User " id "'s path"))

(users-path) ;; => "/users"
(user-path {:id 1}) ;; => "/users/1"
```

This also works with `:query-params`.

```clojure
(user-path {:id 1 :query-params {:action "delete"}})
;; => "/users/1?action=delete"
```

If the browser you're targeting does not support HTML5 history you can
call

```clojure
(secretary/set-config! :prefix "#")
```

to prefix generated URIs with a "#".

```clojure
(user-path {:id 1})
;; => "#/users/1"
```


### Available protocols

You can extend Secretary's protocols to your own data types and
records if you need special functionality.

- [`IRenderRoute`](#irenderroute)
- [`IRouteMatches`](#iroutematches)


#### `IRenderRoute`

Most of the time the defaults will be good enough but on occasion you
may need custom route rendering. To do this implement `IRenderRoute`
for your type or record.

```clojure
(defrecord User [id]
  secretary/IRenderRoute
  (render-route [_]
    (str "/users/" id))

  (render-route [this params]
    (str (secretary/render-route this) "?"
         (secretary/encode-query-params params))))

(secretary/render-route (User. 1))
;; => "/users/1"
(secretary/render-route (User. 1) {:action :delete})
;; => "/users/1?action=delete"
```


#### `IRouteMatches`

It is seldom you will ever need to create your own route matching
implementation as the built in `String` and `RegExp` routes matchers
should be fine for most applications. Still, if you have a suitable
use case then this protocol is available. If your intention is to is
to use it with `defroute` your implementation must return a map or
vector.


### Example with `goog.History`

```clojure
(ns example
  (:require [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(def application
  (js/document.getElementById "application"))

(defn set-html! [el content]
  (aset el "innerHTML" content))

(secretary/set-config! :prefix "#")

;; /#/
(defroute home-path "/" []
  (set-html! application "<h1>OMG! YOU'RE HOME!</h1>"))

;; /#/users
(defroute users-path "/users" []
  (set-html! application "<h1>USERS!</h1>"))

;; /#/users/:id
(defroute user-path "/users/:id" [id]
  (let [message (str "<h1>HELLO USER <small>" id "</small>!</h1>")]
    (set-html! application message)))

;; /#/777
(defroute jackpot-path "/777" []
  (set-html! application "<h1>YOU HIT THE JACKPOT!</h1>"))

;; Catch all
(defroute "*" []
  (set-html! application "<h1>LOL! YOU LOST!</h1>"))

;; Quick and dirty history configuration.
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
```


## Contributors

* [@gf3](https://github.com/gf3) (Gianni Chiappetta)
* [@noprompt](https://github.com/noprompt) (Joel Holdbrooks)
* [@joelash](https://github.com/joelash) (Joel Friedman)
* [@james-henderson](https://github.com/james-henderson) (James Henderson)
* [@the-kenny](https://github.com/the-kenny) (Moritz Ulrich)
* [@timgilbert](https://github.com/timgilbert) (Tim Gilbert)
* [@bbbates](https://github.com/bbbates) (Brendan)
* [@travis](https://github.com/travis) (Travis Vachon)

## Committers

* [@gf3](https://github.com/gf3) (Gianni Chiappetta)
* [@noprompt](https://github.com/noprompt) (Joel Holdbrooks)
* [@joelash](https://github.com/joelash) (Joel Friedman)

## License

Distributed under the Eclipse Public License, the same as Clojure.

[sinatra]: http://www.sinatrarb.com/intro.html#Routes
[rails]: http://guides.rubyonrails.org/routing.html
