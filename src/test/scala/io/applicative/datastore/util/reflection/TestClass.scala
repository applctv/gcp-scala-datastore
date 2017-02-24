package io.applicative.datastore.util.reflection

import java.time.{LocalDateTime, OffsetDateTime, ZoneId, ZonedDateTime}
import java.util.Date

import com.google.cloud.datastore.{Blob, DateTime, LatLng}

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
                      googleDateTimeVal: DateTime = DateTime.now(),
                      latLangVal: LatLng = LatLng.of(42.42, 42.42),
                      blobVal: Blob = Blob.copyFrom(Array[Byte](1, 2, 3)),
                      boolVal: Boolean = true
                    ) {
  def this() = this(Long.MinValue, Byte.MinValue, Int.MinValue, Float.MinValue, Double.MinValue, "",
    new Date(0), LocalDateTime.now(), ZonedDateTime.now(ZoneId.of("UTC").normalized()),
    OffsetDateTime.now(ZoneId.of("UTC").normalized()), DateTime.now(), LatLng.of(4, 2), Blob.copyFrom(Array[Byte](0)),
    false
  )
}
