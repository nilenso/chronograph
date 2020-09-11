(ns chronograph.domain.organization
  (:require [chronograph.db.core :as db]
            [chronograph.db.organization :as db-organization]
            [chronograph.domain.acl :as acl]
            [next.jdbc :as jdbc]
            [chronograph.db.join-requests :as db-join-requests]))

(defn create! [organization owner-id]
  (jdbc/with-transaction [tx db/datasource]
    (let [{:keys [organizations/id] :as organization} (db-organization/create! tx organization)]
      (acl/create! tx {:user-id owner-id
                       :organization-id id
                       :role acl/admin})
      organization)))

(defn find-if-authorized
  [slug user-id]
  (jdbc/with-transaction [tx db/datasource]
    (when-let [{:organizations/keys [id]
                :as organization} (db-organization/find-by-slug tx slug)]
      (when (acl/belongs-to-org? tx
                                 user-id
                                 id)
        organization))))

(def by-id db-organization/by-id)
(def ^:private set-join-secret! db-organization/set-join-secret!)

(defn- gen-join-secret []
  (let [gen-alpha (fn [] (char (+ 97 (rand-nth (range 0 25)))))
        group-length 6
        append-alphas #(dotimes [_ group-length] (.append % (gen-alpha)))
        builder (StringBuilder.)]
    (dotimes [_ 3]
      (append-alphas builder)
      (.append builder \-))
    (append-alphas builder)

    (.toString builder)))

(defn enable-join-requests! [organization-id]
  (jdbc/with-transaction [tx db/datasource]
    (set-join-secret! tx organization-id (gen-join-secret))))

(defn disable-join-requests! [organization-id]
  (jdbc/with-transaction [tx db/datasource]
    (set-join-secret! tx organization-id nil)))

(defn request-join! [user-id organization-id join-secret]
  (jdbc/with-transaction [tx db/datasource]
    (when (db-organization/can-join-organization? tx organization-id join-secret)
      (db-join-requests/create! tx {:user-id user-id
                                    :organization-id organization-id}))))
