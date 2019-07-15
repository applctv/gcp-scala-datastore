package io.applicative.datastore

import com.google.cloud.datastore.{Transaction, Datastore => CloudDatastore}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait Datastore {
  /**
    * Creates a new Key with automatically randomly generated id.
    * @tparam E type of entity. Must be always specified.
    */
  def newKey[E <: BaseEntity : TypeTag : ClassTag]()(implicit ec: ExecutionContext): Future[Key]

  /**
    * Creates a new Key with specified name.
    * @tparam E type of entity. Must be always specified.
    */
  def newKey[E <: BaseEntity : TypeTag : ClassTag](name: String)(implicit ec: ExecutionContext): Future[Key]

  /**
    * Creates a new Key with specified id.
    * @tparam E type of entity. Must be always specified.
    */
  def newKey[E <: BaseEntity : TypeTag : ClassTag](id: Long)(implicit ec: ExecutionContext): Future[Key]

  /**
    * Returns a new Datastore transaction.
    */
  def newTransaction(implicit ec: ExecutionContext): Future[Transaction]

  /**
    * Datastore add operation: inserts the provided entity.
    *
    * If an entity with similar id does not exists, entity is inserted.
    * Otherwise, future fails because of DatastoreException.
    *
    * @param entity instance of type E to be inserted.
    * @tparam E type of entity. Must be always specified.
    */
  def add[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[E]

  /**
    * Datastore add operation: inserts the provided entity with its key.
    *
    * If an entity with similar id does not exists, entity is inserted.
    * Otherwise, future fails because of DatastoreException.
    *
    * @param entity instance of type E to be inserted.
    * @param key Key
    * @tparam E type of entity. Must be always specified.
    */
  def add[E <: BaseEntity : TypeTag : ClassTag](key: Key, entity: E)(implicit ec: ExecutionContext): Future[E]

  /**
    * Datastore add operation: inserts the provided entities along with its keys.
    *
    * If an entity with similar id does not exists, entity is inserted.
    * Otherwise, future fails because of DatastoreException.
    *
    * @param ke map of entity and its key
    * @tparam E type of entity. Must be always specified.
    */
  def add[E <: BaseEntity : TypeTag : ClassTag](ke: Map[Key, E])(implicit ec: ExecutionContext): Future[List[E]]

  /**
    * A Datastore update operation. The operation will fail if an entity with the same id does not
    * already exist.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def update[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[Unit]

  /**
    * A Datastore update operation. The operation will fail if an entity with the same id does not
    * already exist.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def update[E <: BaseEntity : TypeTag : ClassTag](entities: List[E])(implicit ec: ExecutionContext): Future[Unit]

  /**
    * A Datastore put (a.k.a upsert) operation: inserts an entity if it does not exist, updates it
    * otherwise.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def put[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[E]

  /**
    * A Datastore put (a.k.a upsert) operation: inserts an entity if it does not exist, updates it
    * otherwise.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def put[E <: BaseEntity : TypeTag : ClassTag](entities: List[E])(implicit ec: ExecutionContext): Future[List[E]]

  /**
    * A datastore delete operation.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def delete[E <: BaseEntity : TypeTag : ClassTag](keys: Key*)(implicit ec: ExecutionContext): Future[Unit]

  /**
    * A datastore delete operation.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def delete[E <: BaseEntity : TypeTag : ClassTag](ids: List[Long])(implicit ec: ExecutionContext): Future[Unit]

  /**
    * Retrieves instance of class E with specified id.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def get[E <: BaseEntity : TypeTag : ClassTag](id: Long)(implicit ec: ExecutionContext): Future[Option[E]]

  /**
    * Retrieves instance of class E with specified id.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def get[E <: BaseEntity : TypeTag : ClassTag](id: String)(implicit ec: ExecutionContext): Future[Option[E]]

  /**
    * Retrieves instance of class E with specified key.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def get[E <: BaseEntity : TypeTag : ClassTag](key: Key)(implicit ec: ExecutionContext): Future[Option[E]]

  /**
    * Returns an Entity for each given id that exists in the Datastore. The order of
    * the result is unspecified. Results are loaded lazily.
    *
    * @tparam E type of entity. Must be always specified.
    */
  def getLazy[E <: BaseEntity : TypeTag : ClassTag, K](ids: List[K])(implicit ec: ExecutionContext): Future[Iterator[E]]

  /**
    * Returns a list with a value for each given key (ordered by input).
    *
    * @tparam E type of entity. Must be always specified.
    */
  def fetch[E <: BaseEntity : TypeTag : ClassTag, K](ids: List[K])(implicit ec: ExecutionContext): Future[List[Option[E]]]

  private[datastore] def getKindByClass(clazz: Class[_]): String

}
