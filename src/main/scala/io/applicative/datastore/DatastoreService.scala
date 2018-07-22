package io.applicative.datastore

import com.google.cloud.datastore.{DatastoreOptions, DatastoreReader, Entity, EntityQuery, KeyFactory, Transaction, Datastore => CloudDataStore, Key => CloudKey}
import io.applicative.datastore.exception.UnsupportedIdTypeException
import io.applicative.datastore.util.reflection.{Kind, ReflectionHelper}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object DatastoreService extends Datastore with ReflectionHelper {

  private var _cloudDataStore: CloudDataStore = DatastoreOptions.getDefaultInstance.getService

  private val keyFactories = collection.mutable.Map[String, KeyFactory]()

  private def cloudDataStore = _cloudDataStore

  override private[datastore] def setCloudDataStore(cloudDatastore: CloudDataStore): Unit = {
    _cloudDataStore = cloudDatastore
  }

  override def newKey[E <: BaseEntity : TypeTag : ClassTag]()(implicit ec: ExecutionContext): Future[Key] = Future {
    val kind = getKind[E]()
    val incompleteKey = getKeyFactory(kind).newKey()
    Key(cloudDataStore.allocateId(incompleteKey))
  }

  override def newKey[E <: BaseEntity : TypeTag : ClassTag](name: String)(implicit ec: ExecutionContext): Future[Key] = Future {
    val kind = getKind[E]()
    val incompleteKey = getKeyFactory(kind).newKey(name)
    Key(cloudDataStore.allocateId(incompleteKey))
  }

  override def newKey[E <: BaseEntity : TypeTag : ClassTag](id: Long)(implicit ec: ExecutionContext): Future[Key] = Future {
    val kind = getKind[E]()
    val incompleteKey = getKeyFactory(kind).newKey(id)
    Key(cloudDataStore.allocateId(incompleteKey))
  }

  override def add[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[E] = Future {
    val clazz = extractRuntimeClass[E]()
    val kind = getKindByClass(clazz)
    val key = createKey(entity.id, kind)
    val dataStoreEntity = instanceToDatastoreEntity(key, entity, clazz)
    cloudDataStore.add(dataStoreEntity)
    entity
  }

  override def add[E <: BaseEntity : TypeTag : ClassTag](key: Key, entity: E)(implicit ec: ExecutionContext): Future[E] = Future {
    val clazz = extractRuntimeClass[E]()
    val datastoreEntity = instanceToDatastoreEntity(key, entity, clazz)
    val e = cloudDataStore.add(datastoreEntity)
    datastoreEntityToInstance[E](e, clazz)
  }

  override def add[E <: BaseEntity : TypeTag : ClassTag](ke: Map[Key, E])(implicit ec: ExecutionContext): Future[List[E]] = Future {
    val clazz = extractRuntimeClass[E]()
    val entities = ke.map { case (k, v) => instanceToDatastoreEntity(k, v, clazz) }
    val es = cloudDataStore.add(entities.toArray: _*)
    es.toList.map(datastoreEntityToInstance[E](_, clazz))
  }
  override def get[E <: BaseEntity : TypeTag : ClassTag](id: Long)(implicit ec: ExecutionContext): Future[Option[E]] = Future {
    wrapGet[E](id)
  }

  override def get[E <: BaseEntity : TypeTag : ClassTag](id: String)(implicit ec: ExecutionContext): Future[Option[E]] = Future {
    wrapGet[E](id)
  }

  override def get[E <: BaseEntity : TypeTag : ClassTag](key: Key)(implicit ec: ExecutionContext): Future[Option[E]] = Future {
    wrapGet[E](key)
  }

  override def newTransaction(implicit ec: ExecutionContext): Future[Transaction] = Future {
    cloudDataStore.newTransaction()
  }

  override def update[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[Unit] = {
    update[E](List(entity))
  }

  override def update[E <: BaseEntity : TypeTag : ClassTag](entities: List[E])(implicit ec: ExecutionContext): Future[Unit] = Future {
    val es = convert[E](entities)
    cloudDataStore.update(es: _*)
  }

  override def put[E <: BaseEntity : TypeTag : ClassTag](entity: E)(implicit ec: ExecutionContext): Future[E] = {
    put(List(entity)).map(_ => entity)
  }

  override def put[E <: BaseEntity : TypeTag : ClassTag](entities: List[E])(implicit ec: ExecutionContext): Future[List[E]] = Future {
    val es = convert[E](entities)
    cloudDataStore.put(es: _*)
    entities
  }

  override def delete[E <: BaseEntity : TypeTag : ClassTag](keys: Key*)(implicit ec: ExecutionContext): Future[Unit] = Future {
    cloudDataStore.delete(keys.map(_.key): _*)
  }

  override def delete[E <: BaseEntity : TypeTag : ClassTag](ids: List[Long])(implicit ec: ExecutionContext): Future[Unit] = Future {
    val kind = getKind[E]()
    val kf = getKeyFactory(kind)
    val keys = ids.map(i => Key(kf.newKey(i)))
    delete(keys: _*)
  }

  override def getLazy[E <: BaseEntity : TypeTag : ClassTag, K](ids: List[K])(implicit ec: ExecutionContext): Future[Iterator[E]] = Future {
    wrapLazyGet[E](ids)
  }

  override def fetch[E <: BaseEntity : TypeTag : ClassTag, K](ids: List[K])(implicit ec: ExecutionContext): Future[List[Option[E]]] = Future {
    wrapFetch[E](ids)
  }

  private[datastore] def runQueryForSingleOpt[E <: BaseEntity : TypeTag : ClassTag](query: EntityQuery)(implicit ec: ExecutionContext): Future[Option[E]] = Future {
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

  private[datastore] def runQueryForList[E <: BaseEntity : TypeTag : ClassTag](query: EntityQuery)(implicit ec: ExecutionContext): Future[List[E]] = Future {
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

  private def wrapFetch[E: TypeTag : ClassTag](ids: List[_]): List[Option[E]] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKindByClass(clazz)
    val es = ids.map(createKey(_, kind).key)
    val javaIterable: java.lang.Iterable[CloudKey] = es.asJava
    cloudDataStore
      .fetch(javaIterable)
      .asScala
      .map { Option(_).map(datastoreEntityToInstance[E](_, clazz)) }
      .toList
  }

  private def wrapLazyGet[E: TypeTag : ClassTag](ids: List[_]): Iterator[E] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKindByClass(clazz)
    val es = ids.map(createKey(_, kind).key)
    val javaIterable: java.lang.Iterable[CloudKey] = es.asJava
    val scalaIterator = cloudDataStore
      .get(javaIterable)
      .asScala
      .map(datastoreEntityToInstance[E](_, clazz))
    scalaIterator
  }

  private def wrapGet[E: TypeTag : ClassTag](v: Any): Option[E] = {
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

  private[datastore] def getKind[E: ClassTag]() = {
    val clazz = extractRuntimeClass[E]()
    getKindByClass(clazz)
  }

  private[datastore] def getKindByClass(clazz: Class[_]): String = {
    Option(clazz.getDeclaredAnnotation(classOf[Kind])) match {
      case Some(customKeyAnnotation) if customKeyAnnotation.value() != null && customKeyAnnotation.value().nonEmpty =>
        customKeyAnnotation.value()
      case _ =>
        clazz.getCanonicalName
    }
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

  private def convert[E <: BaseEntity : TypeTag : ClassTag](entities: Seq[E]): Seq[Entity] = {
    val clazz = extractRuntimeClass[E]()
    val kind = getKindByClass(clazz)
    entities map { e =>
      val key = createKey(e.id, kind)
      instanceToDatastoreEntity(key, e, clazz)
    }
  }

}
