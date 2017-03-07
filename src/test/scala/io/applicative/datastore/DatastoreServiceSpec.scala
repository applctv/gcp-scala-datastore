package io.applicative.datastore

import com.google.cloud.datastore.{IncompleteKey, KeyFactory, Datastore => CloudDataStore, Key => CloudKey}
import io.applicative.datastore.util.reflection.TestClass
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
  }

  private def mockKeyFactory() = {
    new KeyFactory("mock")
  }

  private def mockKey(kind: String, id: Long) = {
    CloudKey.newBuilder("test", kind, id).build()
  }

}
