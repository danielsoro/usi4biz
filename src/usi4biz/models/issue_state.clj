(ns usi4biz.models.issue-state
  (:require [hikari-cp.core           :refer :all]
            [clojure.java.jdbc        :as jdbc]
            [usi4biz.datasource       :as ds]
            [usi4biz.models.milestone :as milestone]
            [usi4biz.utils.calendar   :as calendar]))

(def states {:CREATED  "CREATED"  ; when an issue is created
             :ASSIGNED "ASSIGNED" ; when an issue has an assignee and a milestone
             :FINISHED "FINISHED" ; when the work on an issue is finished
             :CLOSED   "CLOSED"   ; when the work on an issue is recognized as finished
             :CANCELED "CANCELED" ; when an issue can not be done for any reason
             })

(defrecord issue-state [id issue state set_date])

(defn backlog-size []
 (jdbc/with-db-connection [conn {:datasource ds/datasource}]
   (jdbc/query conn ["select count(id) size
                      from issue
                      where id not in (select issue
                                       from issue_state
                                       where state in ('FINISHED','CLOSED','CANCELED'))"])))

(defn find-by-issue [issue-id]
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (let [rows (jdbc/query conn ["select * from issue_state where issue = ?" issue-id])]
      rows)))

(defn find-total-state-per-month [state]
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (jdbc/query conn ["select year(set_date) year, month(set_date) month, count(issue) total
                       from issue_state
                       where state = ?
                       group by year(set_date), month(set_date)
                       order by set_date" state])))

(defn total-planned-issues [milestone]
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (jdbc/query conn ["select count(i.id) as total
                       from issue i join issue_state iss on i.id = iss.issue
                       where i.milestone = '8B56F3FE55114205A36A39D8D2662F93' and
                             iss.state = 'ASSIGNED' and
                             iss.set_date <= '2015-09-24 16:00:00' and
                             i.assignee is not null"])))

(defn total-unplanned-issues []
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (jdbc/query conn ["select count(iss.id) as total from issue_state iss join issue i on i.id = iss.issue where state = 'ASSIGNED' and set_date > '2015-09-24 16:00:00' and i.milestone = '8B56F3FE55114205A36A39D8D2662F93'"])))

(defn total-finished-issues []
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (jdbc/query conn ["select count(id) as total from issue_state where state = 'FINISHED'"])))

(defn total-unfinished-issues []
  (jdbc/with-db-connection [conn {:datasource ds/datasource}]
    (jdbc/query conn ["select count(id) as total from issue
                       where id not in (select distinct(issue) from issue_state where state = 'FINISHED' or state = 'CANCELED' or state = 'CLOSED') and
                      milestone = '8B56F3FE55114205A36A39D8D2662F93'"])))

(defn accumulate-total-per-month [state]
  (let [totals (find-total-state-per-month state)]
    (loop [totals                  totals
           accumulated             (:total (first totals))
           totals-with-accumulated []]
      (if (empty? totals)
        totals-with-accumulated
        (recur (rest totals)
               (+ accumulated (if (<= (count totals) 1)
                                  0
                                  (:total (second totals))))
               (conj totals-with-accumulated (assoc (first totals)
                                                    :accumulated accumulated)))))))

(defn create [a-issue-state]
  (let [issue-state (assoc issue-state :id (ds/unique-id))]
    (jdbc/insert! ds/db-spec :issue_state issue-state)
    issue-state))
