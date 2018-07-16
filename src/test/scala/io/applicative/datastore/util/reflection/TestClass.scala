package io.applicative.datastore.util.reflection

import java.time.{LocalDateTime, OffsetDateTime, ZoneId, ZonedDateTime}
import java.util.Date

import com.google.cloud.Timestamp
import com.google.cloud.datastore.{Blob, LatLng}
import io.applicative.datastore.BaseEntity

case class TestClass(
                      id: Long = Long.MaxValue,
                      byteVal: Byte = Byte.MaxValue,
                      intVal: Int = Int.MaxValue,
                      floatVal: Float = Float.MaxValue,
                      doubleVal: Double = Double.MaxValue,
                      stringVal: String = "testString",
                      javaUtilDateVal: Date = new Date(),
                      localDateTimeVal: LocalDateTime = LocalDateTime.now(),
                      zonedDateTimeVal: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC").normalized()),
                      offsetDateTimeVal: OffsetDateTime = OffsetDateTime.now(ZoneId.of("UTC").normalized()),
                      googleDateTimeVal: Timestamp = Timestamp.of(new Date()),
                      latLangVal: LatLng = LatLng.of(42.42, 42.42),
                      blobVal: Blob = Blob.copyFrom(Array[Byte](1, 2, 3)),
                      boolVal: Boolean = true,
                      adt1: Adt = Type1,
                      adt2: Adt = Type2(Long.MaxValue)
                    ) extends BaseEntity

sealed trait Adt
case object Type1 extends Adt
case class Type2(id: Long) extends Adt
