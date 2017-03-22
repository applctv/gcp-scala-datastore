package io.applicative.datastore

import com.google.cloud.datastore.{Transaction, Datastore => CloudDatastore}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

trait Datastore {
  // for testing purpose
  private[datastore] def setCloudDataStore(cloudDatastore: CloudDatastore): Unit

  def newKey[E <: BaseEntity : TypeTag : ClassTag]()(implicit ec: ExecutionContext): Future[Key]

  def newKey[E <: BaseEntity : TypeTag : ClassTag](name: String)(implicit ec: ExecutionContext): Future[Key]

  def newKey[E <: BaseEntity : TypeTag : ClassTag](id: Long)(implicit ec: ExecutionContext): Future[Key]

  def newTransaction(implicit ec: ExecutionContext): Future[Transaction]

  def add[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[E]

  def add[E <: BaseEntity : TypeTag : ClassTag](key: Key, entity: E)(implicit ec: ExecutionContext): Future[E]

  def add[E <: BaseEntity : TypeTag : ClassTag](ke: Map[Key, E])(implicit ec: ExecutionContext): Future[List[E]]

  def update[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[Unit]

  def update[E <: BaseEntity : TypeTag : ClassTag](entities: List[E])(implicit ec: ExecutionContext): Future[Unit]

  def put[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[E]

  def put[E <: BaseEntity : TypeTag : ClassTag](entities: List[E])(implicit ec: ExecutionContext): Future[List[E]]

  def delete[E <: BaseEntity : TypeTag : ClassTag](keys: Key*)(implicit ec: ExecutionContext): Future[Unit]

  def delete[E <: BaseEntity : TypeTag : ClassTag](ids: List[Long])(implicit ec: ExecutionContext): Future[Unit]

  def get[E <: BaseEntity : TypeTag : ClassTag](id: Long)(implicit ec: ExecutionContext): Future[Option[E]]

  def get[E <: BaseEntity : TypeTag : ClassTag](id: String)(implicit ec: ExecutionContext): Future[Option[E]]

  def get[E <: BaseEntity : TypeTag : ClassTag](key: Key)(implicit ec: ExecutionContext): Future[Option[E]]

  def getLazy[E <: BaseEntity : TypeTag : ClassTag, K](ids: List[K])(implicit ec: ExecutionContext): Future[Iterator[E]]

  def fetch[E <: BaseEntity : TypeTag : ClassTag, K](ids: List[K])(implicit ec: ExecutionContext): Future[List[Option[E]]]

}
