package io.applicative.datastore

import com.google.cloud.datastore.{IncompleteKey, KeyFactory, Datastore => CloudDataStore, Key => CloudKey}
import io.applicative.datastore.util.reflection.{DatastoreKey, TestClass}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification


class DatastoreServiceSpec extends Specification with Mockito {
  private val cloudDataStore = mock[CloudDataStore]
  private val dataStoreService = DatastoreService
  dataStoreService.setCloudDataStore(cloudDataStore)
  private val testInstance = TestClass()
  type EE = ExecutionEnv

  "DatastoreService should" should {

    "create a new key with specified Kind" in { implicit ee: EE =>
      cloudDataStore.newKeyFactory() returns mockKeyFactory()
      val mk = mockKey("TestClass", testInstance.id)
      cloudDataStore.allocateId(any[IncompleteKey]) returns mk
      val key = dataStoreService.newKey[TestClass]()
      key must beEqualTo(Key(mk)).await
    }

    "create a new key with class name as a value if annotation DatastoreKey is not present" in { implicit ee: EE =>
      val clazz = classOf[SomeEntity]
      val kind = dataStoreService.getKindByClass(clazz)
      kind must beEqualTo("io.applicative.datastore.SomeEntity")
    }

    "create a new key with custom value if annotation DatastoreKey is present" in { implicit ee: EE =>
      val clazz = classOf[SomeEntity2]
      val kind = dataStoreService.getKindByClass(clazz)
      kind must beEqualTo("CustomKey")
    }
  }

  private def mockKeyFactory() = {
    new KeyFactory("mock")
  }

  private def mockKey(kind: String, id: Long) = {
    CloudKey.newBuilder("test", kind, id).build()
  }

}

case class SomeEntity(id: Long)
@DatastoreKey(value = "CustomKey")
case class SomeEntity2(id: Long)
