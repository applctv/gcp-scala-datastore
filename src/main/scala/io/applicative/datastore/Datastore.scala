package io.applicative.datastore

import com.google.cloud.datastore.{Transaction, Datastore => CloudDatastore}

import scala.concurrent.Future
import scala.reflect.runtime.universe._

trait Datastore {
  // for testing purpose
  private[datastore] def setCloudDataStore(cloudDatastore: CloudDatastore): Unit

  def newKey[E: TypeTag](): Future[Key]

  def newKey[E: TypeTag](name: String): Future[Key]

  def newKey[E: TypeTag](id: Long): Future[Key]

  def newTransaction: Future[Transaction]

  def add[E: TypeTag](key: Key, entity: E): Future[E]

  def add[E: TypeTag](ke: Map[Key, E]): Future[List[E]]

  def update[E <: BaseEntity : TypeTag](entity: E): Future[Unit]

  def update[E <: BaseEntity : TypeTag](entities: List[E]): Future[Unit]

  def put[E <: BaseEntity : TypeTag](entity: E): Future[E]

  def put[E <: BaseEntity : TypeTag](entities: List[E]): Future[List[E]]

  def delete[E: TypeTag](keys: Key*): Future[Unit]

  def delete[E: TypeTag](ids: List[Long]): Future[Unit]

  def get[E: TypeTag](id: Long): Future[Option[E]]

  def get[E: TypeTag](id: String): Future[Option[E]]

  def get[E: TypeTag](key: Key): Future[Option[E]]

  def getLazy[E: TypeTag, K](ids: List[K]): Future[Iterator[E]]

  def fetch[E: TypeTag, K](ids: List[K]): Future[List[Option[E]]]

}
