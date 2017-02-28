package io.applicative.datastore

import com.google.cloud.datastore.{PathElement, Key => CloudKey}

import scala.collection.JavaConverters._

final case class Key(key: CloudKey) {
  assert(key != null)
  def kind: String = key.getKind
  def id: Option[Long] = Option(key.getId)
  def name: Option[String] = Option(key.getName)
  def nameOrId: Either[String, Long] = key.getNameOrId match {
    case name: String => Left(name)
    case id: java.lang.Long => Right(id)
  }
  def ancestors: List[PathElement] = key.getAncestors.asScala.toList
  def hasId: Boolean = key.hasId
  def hasName: Boolean = key.hasName
  def toUrlSafe: String = key.toUrlSafe
  def parent: Option[Key] = Option(key.getParent).map(Key)
  def namespace: Option[String] = Option(key.getNamespace)
  def projectId: String = key.getProjectId
}