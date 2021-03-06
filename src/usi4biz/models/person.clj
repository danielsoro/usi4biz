; Usi4Biz: User Interaction For Business
; Copyright (C) 2015 Hildeberto Mendonça
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program. If not, see http://www.gnu.org/licenses/.

(ns ^{:author "Hildeberto Mendonça - hildeberto.com"}
  usi4biz.models.person
  (:require [hikari-cp.core     :refer :all]
            [clojure.java.jdbc  :as jdbc]
            [usi4biz.datasource :as ds]
            [bouncer.validators :as v]))

(def validation-rules {:first_name v/required
                       :last_name  v/required
                       :email      v/required})

(defn find-all []
 (jdbc/with-db-connection [conn {:datasource ds/datasource}]
   (jdbc/query conn ["select   p.id, p.first_name, p.last_name, p.email, p.user_account, u.username
                      from     person p left join user_account u on p.user_account = u.id
                      order by first_name asc"])))

(defn find-it [id]
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (first (jdbc/query conn ["select p.id, p.first_name, p.last_name, p.email, p.user_account, u.username
                              from   person p left join user_account u on p.user_account = u.id
                              where  p.id = ?" id]))))

(defn save [a-person]
  (if (empty? (:id a-person))
    (let [person (assoc a-person :id (ds/unique-id))]
      (jdbc/insert! ds/db-spec :person person)
      person)
    (let [person a-person]
      (jdbc/update! ds/db-spec :person person ["id = ?" (:id person)])
      person)))

(defn delete [id]
  (jdbc/delete! ds/db-spec :person ["id = ?" id]) id)
