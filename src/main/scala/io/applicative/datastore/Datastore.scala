package io.applicative.datastore

import com.google.cloud.datastore.Transaction

import scala.concurrent.Future
import scala.reflect.runtime.universe._

trait Datastore {
  def newKey[E: TypeTag](): Future[Key[E]]

  def newKey[E: TypeTag](name: String): Future[Key[E]]

  def newKey[E: TypeTag](id: Long): Future[Key[E]]

  def newTransaction: Future[Transaction]

  def add[E](key: Key[E], entity: E): Future[E]

  def add[E](entities: E*): Future[List[E]]

  def update[E](entity: E): Future[Unit]

  def update[E](entities: E*): Future[Unit]

  def put[E](entity: E): Future[E]

  def put[E](entities: E*): Future[List[E]]

  def delete[E](keys: Key[E]*): Future[Unit]

  def get[E: TypeTag](id: Long): Future[Option[E]]

  def get[E: TypeTag](id: String): Future[Option[E]]

  def get[E: TypeTag](key: Key[E]): Future[Option[E]]

  def get[E: TypeTag](keys: Iterable[Key[E]]): Future[Iterator[E]]

  def fetch[E: TypeTag](keys: Iterable[Key[E]]): Future[List[Option[E]]]

}
