package io.applicative.datastore

import com.google.cloud.datastore.{DatastoreOptions, DatastoreReader, Entity, EntityQuery, KeyFactory, Transaction,
                                   Datastore => CloudDataStore, Key => CloudKey}
import io.applicative.datastore.exception.UnsupportedIdTypeException
import io.applicative.datastore.util.reflection.ReflectionHelper

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.runtime.universe._

object DatastoreService extends Datastore with ReflectionHelper {

  private var _cloudDataStore: CloudDataStore = DatastoreOptions.getDefaultInstance.getService

  private val keyFactories = collection.mutable.Map[String, KeyFactory]()

  private def cloudDataStore = _cloudDataStore

  override private[datastore] def setCloudDataStore(cloudDatastore: CloudDataStore): Unit = {
    _cloudDataStore = cloudDatastore
  }

  override def newKey[E <: BaseEntity : TypeTag](): Future[Key] = Future {
    val kind = getKind[E]()
    val incompleteKey = getKeyFactory(kind).newKey()
    Key(cloudDataStore.allocateId(incompleteKey))
  }

  override def newKey[E <: BaseEntity : TypeTag](name: String): Future[Key] = Future {
    val kind = getKind[E]()
    val incompleteKey = getKeyFactory(kind).newKey(name)
    Key(cloudDataStore.allocateId(incompleteKey))
  }

  override def newKey[E <: BaseEntity : TypeTag](id: Long): Future[Key] = Future {
    val kind = getKind[E]()
    val incompleteKey = getKeyFactory(kind).newKey(id)
    Key(cloudDataStore.allocateId(incompleteKey))
  }

  override def add[E <: BaseEntity : TypeTag](entity: E): Future[E] = Future {
    val clazz = extractRuntimeClass[E]()
    val kind = getKind(clazz)
    val key = createKey(entity.id, kind)
    val dataStoreEntity = instanceToDatastoreEntity(key, entity, clazz)
    cloudDataStore.add(dataStoreEntity)
    entity
  }

  override def add[E <: BaseEntity : TypeTag](key: Key, entity: E): Future[E] = Future {
    val clazz = extractRuntimeClass[E]()
    val datastoreEntity = instanceToDatastoreEntity(key, entity, clazz)
    val e = cloudDataStore.add(datastoreEntity)
    datastoreEntityToInstance[E](e, clazz)
  }

  override def add[E <: BaseEntity : TypeTag](ke: Map[Key, E]): Future[List[E]] = Future {
    cloudDataStore.update()
    val clazz = extractRuntimeClass[E]()
    val entities = ke.map { case (k, v) => instanceToDatastoreEntity(k, v, clazz) }
    val es = cloudDataStore.add(entities.toArray: _*)
    es.toList.map(datastoreEntityToInstance[E](_, clazz))
  }
  override def get[E <: BaseEntity : TypeTag](id: Long): Future[Option[E]] = Future {
    wrapGet[E](id)
  }

  override def get[E <: BaseEntity : TypeTag](id: String): Future[Option[E]] = Future {
    wrapGet[E](id)
  }

  override def get[E <: BaseEntity : TypeTag](key: Key): Future[Option[E]] = Future {
    wrapGet[E](key)
  }

  override def newTransaction: Future[Transaction] = Future {
    cloudDataStore.newTransaction()
  }

  override def update[E <: BaseEntity : TypeTag](entity: E): Future[Unit] = {
    update[E](List(entity))
  }

  override def update[E <: BaseEntity : TypeTag](entities: List[E]): Future[Unit] = Future {
    val es = convert[E](entities)
    cloudDataStore.update(es: _*)
  }

  override def put[E <: BaseEntity : TypeTag](entity: E): Future[E] = {
    put(List(entity)).map(_ => entity)
  }

  override def put[E <: BaseEntity : TypeTag](entities: List[E]): Future[List[E]] = Future {
    val es = convert[E](entities)
    cloudDataStore.put(es: _*)
    entities
  }

  override def delete[E <: BaseEntity : TypeTag](keys: Key*): Future[Unit] = Future {
    cloudDataStore.delete(keys.map(_.key): _*)
  }

  override def delete[E <: BaseEntity : TypeTag](ids: List[Long]): Future[Unit] = Future {
    val kind = getKind[E]()
    val kf = getKeyFactory(kind)
    val keys = ids.map(i => Key(kf.newKey(i)))
    delete(keys: _*)
  }

  override def getLazy[E <: BaseEntity : TypeTag, K](ids: List[K]): Future[Iterator[E]] = Future {
    wrapLazyGet[E](ids)
  }

  override def fetch[E <: BaseEntity : TypeTag, K](ids: List[K]): Future[List[Option[E]]] = Future {
    wrapFetch[E](ids)
  }

  private[datastore] def runQueryForSingleOpt[E <: BaseEntity : TypeTag](query: EntityQuery): Option[E] = {
    val clazz = extractRuntimeClass[E]()
    val cloudDataStoreReader: DatastoreReader = cloudDataStore
    val results = cloudDataStoreReader.run(query)
    if (results.hasNext) {
      val entity = results.next()
      Some(datastoreEntityToInstance[E](entity, clazz))
    } else {
      None
    }
  }

  private[datastore] def runQueryForList[E <: BaseEntity : TypeTag](query: EntityQuery): List[E] = {
    val clazz = extractRuntimeClass[E]()
    val cloudDataStoreReader: DatastoreReader = cloudDataStore
    val results = cloudDataStoreReader.run(query)
    @tailrec
    def iter(list: List[E]): List[E] = {
      if (results.hasNext) {
        iter(list:+ datastoreEntityToInstance[E](results.next, clazz))
      } else {
        list
      }
    }
    iter(List())
  }

  private def wrapFetch[E: TypeTag](ids: List[_]): List[Option[E]] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKind(clazz)
    val es = ids.map(createKey(_, kind).key)
    val javaIterable: java.lang.Iterable[CloudKey] = es.asJava
    cloudDataStore
      .fetch(javaIterable)
      .asScala
      .map { Option(_).map(datastoreEntityToInstance[E](_, clazz)) }
      .toList
  }

  private def wrapLazyGet[E: TypeTag](ids: List[_]): Iterator[E] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKind(clazz)
    val es = ids.map(createKey(_, kind).key)
    val javaIterable: java.lang.Iterable[CloudKey] = es.asJava
    val scalaIterator = cloudDataStore
      .get(javaIterable)
      .asScala
      .map(datastoreEntityToInstance[E](_, clazz))
    scalaIterator
  }

  private def wrapGet[E: TypeTag](v: Any): Option[E] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKind[E]()
    val cloudDataStoreReader: DatastoreReader = cloudDataStore
    val key = v match {
      case id: Long => getKeyFactory(kind).newKey(id)
      case id: String => getKeyFactory(kind).newKey(id)
      case key: Key => key.key
    }
    val entity = Option(cloudDataStoreReader.get(key))
    entity.map(datastoreEntityToInstance[E](_, clazz))
  }

  private def getKeyFactory(kind: String) = {
    keyFactories.getOrElse(kind, {
      val keyFactory = cloudDataStore.newKeyFactory().setKind(kind)
      keyFactories.put(kind, keyFactory)
      keyFactory
    })
  }

  private def getKind[E: TypeTag]() = {
    extractRuntimeClass[E]().getCanonicalName
  }

  private def getKind(clazz: Class[_]) = {
    clazz.getCanonicalName
  }

  private def createKey(id: Any, kind: String) = {
    val cloudKey = id match {
      case id: String => getKeyFactory(kind).newKey(id)
      case id: Long => getKeyFactory(kind).newKey(id)
      case id: Int => getKeyFactory(kind).newKey(id)
      case otherId => throw UnsupportedIdTypeException(otherId.getClass.getCanonicalName)
    }
    Key(cloudKey)
  }

  private def convert[E <: BaseEntity : TypeTag](entities: Seq[E]): Seq[Entity] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKind(clazz)
    entities map { e =>
      val key = createKey(e.id, kind)
      instanceToDatastoreEntity(key, e, clazz)
    }
  }

}
