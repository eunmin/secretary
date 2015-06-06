# secretary

클로저스크립트에서 사용하는 클라이언트 라우터

[ ![Codeship Status for gf3/secretary](https://codeship.io/projects/0b3ff6f0-ca42-0131-5e5a-5e2103a31145/status?branch=master)](https://codeship.io/projects/22531)


## Contents

- [설치](#설치)
- [가이드](#가이드)
  * [기본적인 라우팅과 디스패치](#기본적인 라우팅과 디스패치)
  * [라우트 매쳐](#라우트 매쳐)
  * [파라메터 디스트럭처링](#파라메터 디스트럭처링)
  * [쿼리 파라메터](#쿼리 파라메터)
  * [함수로 쓸수 있는 라우터](#함수로 쓸수 있는 라우터)
- [History를 사용한 예](#History를 사용한 예)
- [프로토콜](#프로토콜)
- [컨트리뷰터](#컨트리뷰터)
- [커미터](#커미터)


## 설치

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


### 기본적인 라우팅과 디스패치

Secretary는 두가지 목적을 위해 만들어졌다: 라우트 매쳐를 생성하고 
알맞은 액션을 실행하는 것이다.
라우터 매쳐는 기본적인 매칭기능과 URI에서 파라메터 가져오는 기능, 
그리고 함수에서 그 파라메터를 사용할 수 있도록 해준다.

`defroute`는 Secretary의 라우트 매쳐와 액션을 연결해주는 기본 매크로다.
`defroute` 매크로의 시그네쳐는 `[name? route destruct & body]`다.
`name?`은 [named routes](#named-routes)을 다룰때 설명할 것이기 
때문에 지금은 무시하자.
이해를 돕기 위해 id 파라미터를 받은 users 라우트를 정의해보자.

```clojure
(defroute "/users/:id" {:as params}
  (js/console.log (str "User: " (:id params))))
```

여기서 `"/users/:id"`는 라우트 매쳐라고 부르고 있는 `route`다.
`{:as params}`는 라우트 매쳐의 매칭된 결과에서 파라메터 값을 
디스트럭처링 하는 `destruct`다.
그리고 나머지 부분은 라우트 액션인 `body`다.

더 자세히 알아보기 전에 지금 만든 라우터를 디스패치해서 확인해보자.

```clojure
(secretary/dispatch! "/users/gf3")
```

별 문제가 없다면 페이지를 새로고침 하면 콘솔창에 
`User: gf3`라고 나오는 것을 볼 수 있다.

#### 라우트 매쳐

기본적으로 라우트 메쳐는 문자열이나 정규식을 쓸 수 있다.
문자열을 사용하는 경우 [Sinatra][sinatra]나 [Ruby on Rails][rails]를 
사용해 본 사람들은 익숙할 만한 형태를 가지고 있다.
특정 URI를 가지고 `secretary/dispatch!`를 부르면 라우트는 URI에 매칭되는 
라우트를 찾고 그 라우트에 연결된 액션을 수행 한다.
라우트를 찾으면 URI에서 파라메터도 가져올 수 있다.
문자열 라우트의 경우에는 이 파라메터들은 맵 형식을 가지고 
정규식 라우트인 경우에는 벡터로 표현된다.

위의 예에서 `"/users/gf3"` URI는 `"/users/:id"` 라우트에 매칭되고 
`{:id "gf3}` 라는 파라메터 맵을 가진다.
아래는 다양한 라우트 매쳐와 URI와 매칭이 되었을 때의 
파라메터 맵을 보여주고 있다.

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


#### 파라메터 디스트럭처링

앞에서 기본적인 라우터 디스패칭에 대해서 알아봤다. 
이제 `defroute`에 있는 `destruct` 아규먼트에 대해서 알아보자.
이 부분은 `let` 구문으로 감싼 것처럼 동작한다.
기본적으로 라우트 메처가 매칭이 되면 파라메터를 가져오고 가져온 
파라메터를 디스트럭처링 한다.
앞에서 만들었던 users 라우트는 내부적으로 아래와 같이 동작한다:

```clojure
(let [{:as params} {:id "gf3"}]
  ...)
```

따라서 아래와 같이 사용할 수 있고

```clojure
(defroute "/users/:id" {id :id}
  (js/console.log (str "User: " id)))
```

같은 결과가 나온다.

문자열 라우트 메쳐를 사용한다면 이렇게 쓸 수도 있다:

```clojure
(defroute "/users/:id" [id]
  (js/console.log (str "User: " id)))
```

위의 표현은 `{:keys [id]}`와 같다.

정규식 라우트 메쳐를 사용한다면 파라미터가 항상 벡터로 리턴되기 때문에 
디스트럭처링하려면 벡터만 사용할 수 있다.

```clojure
(defroute #"/users/(\d+)" [id]
  (js/console.log (str "User: " id)))
```


#### 쿼리 파라메터

URI에 쿼리 스트링이 있다면 문자열 라우트 메쳐에서는 `:query-params` 키에
쿼리 스트링이 맵으로 들어오고 정규식 라우트 메쳐에서는 벡터의 
마지막 파라미터에 쿼리 스트링 맵이 들어온다.

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


#### 함수로 쓸수 있는 라우터

지금까지 살펴본 라우트 매칭과 디스패치는 충분히 유용하다. 
하지만 많은 경우에 파라메터 맵 파라메터를 가지고 받고 URI를 리턴하는 함수가 필요할 때가 있다.
이를 위해 `defroute` 매크로는 함수 이름을 넘겨서 이런 함수를 자동으로 만들 수 있다.

```clojure
(defroute users-path "/users" []
  (js/console.log "Users path"))

(defroute user-path "/users/:id" [id]
  (js/console.log (str "User " id "'s path"))

(users-path) ;; => "/users"
(user-path {:id 1}) ;; => "/users/1"
```

`:query-params`도 사용할 수 있다:

```clojure
(user-path {:id 1 :query-params {:action "delete"}})
;; => "/users/1?action=delete"
```

브러우저가 HTML5 히스토리를 지원하지 았는다면 아래와 같이 
"#" Prefix를 만들어 쓸 수 있다.

```clojure
(secretary/set-config! :prefix "#")
```

```clojure
(user-path {:id 1})
;; => "#/users/1"
```


### 프로토콜

필요한 기능이 있다면 데이터나 레코드 타입에 Secretary 프로토콜을
확장할 수 있다.

- [`IRenderRoute`](#irenderroute)
- [`IRouteMatches`](#iroutematches)


#### `IRenderRoute`

대부분의 경우에는 그대로 사용해도 충분하지만 특정 타입에 대해 커스텀 랜더링을
할 수 있으면 편리하다. 이때 사용할 수 있는 프로토콜이 `IRenderRoute`이다.

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

이 프로토콜은 이미 만들어져 있는 `String`이나 `RegExp` 처럼 따로 
라우트 매처를 정의 할 수 있는 프로토콜로 대부분의 어플리케이션에는 
거의 필요하지 않다. 그래도 이 프로토콜을 사용할 수 있게 해 놓았다.
`defroute`에서 이 프로토콜을 사용한다면 맵이나 백터를 리턴하도록 구현해야한다.

### History를 사용한 예

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


## 컨트리뷰터

* [@gf3](https://github.com/gf3) (Gianni Chiappetta)
* [@noprompt](https://github.com/noprompt) (Joel Holdbrooks)
* [@joelash](https://github.com/joelash) (Joel Friedman)
* [@james-henderson](https://github.com/james-henderson) (James Henderson)
* [@the-kenny](https://github.com/the-kenny) (Moritz Ulrich)
* [@timgilbert](https://github.com/timgilbert) (Tim Gilbert)
* [@bbbates](https://github.com/bbbates) (Brendan)
* [@travis](https://github.com/travis) (Travis Vachon)

## 커미터

* [@gf3](https://github.com/gf3) (Gianni Chiappetta)
* [@noprompt](https://github.com/noprompt) (Joel Holdbrooks)
* [@joelash](https://github.com/joelash) (Joel Friedman)

## 라이센스

Distributed under the Eclipse Public License, the same as Clojure.

[sinatra]: http://www.sinatrarb.com/intro.html#Routes
[rails]: http://guides.rubyonrails.org/routing.html
