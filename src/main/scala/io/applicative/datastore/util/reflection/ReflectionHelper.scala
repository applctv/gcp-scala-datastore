package io.applicative.datastore.util.reflection

import java.time.{LocalDateTime, OffsetDateTime, ZonedDateTime}
import java.util.Date

import com.google.cloud.datastore.{Blob, DateTime, Entity, LatLng}
import io.applicative.datastore.Key
import io.applicative.datastore.exception.UnsupportedFieldTypeException
import io.applicative.datastore.util.DateTimeHelper

import scala.reflect.runtime.universe._

private[datastore] trait ReflectionHelper extends DateTimeHelper {

  val ByteClassName = classOf[Byte].getSimpleName
  val IntClassName = classOf[Int].getSimpleName
  val LongClassName = classOf[Long].getSimpleName
  val FloatClassName = classOf[Float].getSimpleName
  val DoubleClassName = classOf[Double].getSimpleName
  val StringClassName = classOf[String].getSimpleName
  val JavaUtilDateClassName = classOf[Date].getSimpleName
  val BooleanClassName = classOf[Boolean].getSimpleName
  val DatastoreDateTimeClassName = classOf[DateTime].getSimpleName
  val DatastoreLatLongClassName = classOf[LatLng].getSimpleName
  val DatastoreBlobClassName = classOf[Blob].getSimpleName
  val LocalDateTimeClassName = classOf[LocalDateTime].getSimpleName
  val ZonedDateTimeClassName = classOf[ZonedDateTime].getSimpleName
  val OffsetDateTimeClassName = classOf[OffsetDateTime].getSimpleName

  def extractRuntimeClass[E: TypeTag](): RuntimeClass = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    mirror.runtimeClass(typeOf[E].typeSymbol.asClass)
  }

  def instanceToDatastoreEntity[E](key: Key[E], classInstance: E, clazz: Class[_]): Entity = {
    var builder = Entity.newBuilder(key.key)
    clazz.getDeclaredFields
      .filterNot(_.isSynthetic)
      .tail
      .map(f => {
        f.setAccessible(true)
        Field(f.getName, f.get(classInstance))
      })
      .foreach {
        case Field(name, value: Boolean) => builder = builder.set(name, value)
        case Field(name, value: Byte) => builder = builder.set(name, value)
        case Field(name, value: Int) => builder = builder.set(name, value)
        case Field(name, value: Long) => builder = builder.set(name, value)
        case Field(name, value: Float) => builder = builder.set(name, value)
        case Field(name, value: Double) => builder = builder.set(name, value)
        case Field(name, value: String) => builder = builder.set(name, value)
        case Field(name, value: Date) => builder = builder.set(name, toMilliSeconds(value))
        case Field(name, value: DateTime) => builder = builder.set(name, value)
        case Field(name, value: LocalDateTime) => builder = builder.set(name, formatLocalDateTime(value))
        case Field(name, value: OffsetDateTime) => builder = builder.set(name, formatOffsetDateTime(value))
        case Field(name, value: ZonedDateTime) => builder = builder.set(name, formatZonedDateTime(value))
        case Field(name, value: LatLng) => builder = builder.set(name, value)
        case Field(name, value: Blob) => builder = builder.set(name, value)
        case Field(name, value) => throw UnsupportedFieldTypeException(value.getClass.getCanonicalName)
      }
    builder.build()
  }

  def datastoreEntityToInstance[E](entity: Entity, clazz: Class[_]): E = {
    //TODO: Try to get rid of default constructor requirement
    val defaultInstance = clazz.newInstance()
    val fields = clazz.getDeclaredFields.filterNot(_.isSynthetic)
    val idField = fields.head
    idField.setAccessible(true)
    idField.set(defaultInstance, entity.getKey.getId)
    fields.tail
      .foreach(f => {
        f.setAccessible(true)
        val value = f.getType.getSimpleName match {
          case ByteClassName => entity.getLong(f.getName).toByte
          case IntClassName => entity.getLong(f.getName).toInt
          case LongClassName => entity.getLong(f.getName)
          case StringClassName => entity.getString(f.getName)
          case FloatClassName => entity.getDouble(f.getName).toFloat
          case DoubleClassName => entity.getDouble(f.getName)
          case BooleanClassName => entity.getBoolean(f.getName)
          case JavaUtilDateClassName => toJavaUtilDate(entity.getLong(f.getName))
          case DatastoreDateTimeClassName => entity.getDateTime(f.getName)
          case LocalDateTimeClassName => parseLocalDateTime(entity.getString(f.getName))
          case ZonedDateTimeClassName => parseZonedDateTime(entity.getString(f.getName))
          case OffsetDateTimeClassName => parseOffsetDateTime(entity.getString(f.getName))
          case DatastoreLatLongClassName => entity.getLatLng(f.getName)
          case DatastoreBlobClassName => entity.getBlob(f.getName)
          case fieldName => throw UnsupportedFieldTypeException(fieldName)
        }
        f.set(defaultInstance, value)
      })
    defaultInstance.asInstanceOf[E]
  }
}
