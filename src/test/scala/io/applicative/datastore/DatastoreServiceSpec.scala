package io.applicative.datastore

import com.google.cloud.datastore.{IncompleteKey, KeyFactory, Datastore => CloudDataStore, Key => CloudKey}
import io.applicative.datastore.util.reflection.{Kind, TestClass}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification


class DatastoreServiceSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {
  private val cloudDataStore = mock[CloudDataStore]
  private val dataStoreService = DatastoreService(cloudDataStore)
  private val testInstance = TestClass()

  "DatastoreService should" should {

    "create a new key with specified Kind" in {
      cloudDataStore.newKeyFactory() returns mockKeyFactory()
      val mk = mockKey("TestClass", testInstance.id)
      cloudDataStore.allocateId(any[IncompleteKey]) returns mk
      val key = dataStoreService.newKey[TestClass]()
      key must beEqualTo(Key(mk)).await
    }

    "create a new key with class name as a value if annotation Kind is not present" in {
      val clazz = classOf[SomeEntity]
      val kind = dataStoreService.getKindByClass(clazz)
      kind must beEqualTo("io.applicative.datastore.SomeEntity")
    }

    "create a new key with custom value if annotation Kind is present" in {
      val clazz = classOf[SomeEntity2]
      val kind = dataStoreService.getKindByClass(clazz)
      kind must beEqualTo("CustomKind")
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
@Kind(value = "CustomKind")
case class SomeEntity2(id: Long)
