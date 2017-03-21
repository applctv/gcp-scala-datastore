package io.applicative.datastore.util.reflection

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

import com.google.cloud.datastore.{Blob, DateTime, Entity, LatLng}
import io.applicative.datastore.Key
import io.applicative.datastore.exception.{MissedTypeParameterException, UnsupportedFieldTypeException}
import io.applicative.datastore.util.DateTimeHelper

import scala.reflect.runtime.universe._

private[datastore] trait ReflectionHelper extends DateTimeHelper {

  private val ByteClassName = classOf[Byte].getCanonicalName
  private val IntClassName = classOf[Int].getCanonicalName
  private val LongClassName = classOf[Long].getCanonicalName
  private val FloatClassName = classOf[Float].getCanonicalName
  private val DoubleClassName = classOf[Double].getCanonicalName
  private val StringClassName = classOf[String].getCanonicalName
  private val JavaUtilDateClassName = classOf[Date].getCanonicalName
  private val BooleanClassName = classOf[Boolean].getCanonicalName
  private val DatastoreDateTimeClassName = classOf[DateTime].getCanonicalName
  private val DatastoreLatLongClassName = classOf[LatLng].getCanonicalName
  private val DatastoreBlobClassName = classOf[Blob].getCanonicalName
  private val LocalDateTimeClassName = classOf[LocalDateTime].getCanonicalName
  private val ZonedDateTimeClassName = classOf[ZonedDateTime].getCanonicalName
  private val OffsetDateTimeClassName = classOf[OffsetDateTime].getCanonicalName

  private[datastore] def extractRuntimeClass[E: TypeTag](): RuntimeClass = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val runtimeClass = mirror.runtimeClass(typeOf[E].typeSymbol.asClass)
    if (runtimeClass == classOf[Nothing]) {
      throw MissedTypeParameterException()
    }
    runtimeClass
  }

  private[datastore] def instanceToDatastoreEntity[E](key: Key, classInstance: E, clazz: Class[_]): Entity = {
    var builder = Entity.newBuilder(key.key)
    clazz.getDeclaredFields
      .filterNot(_.isSynthetic)
      // Take all fields except the first one assuming it is an ID field, which is already encapsulated in the Key
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

  private[datastore] def datastoreEntityToInstance[E](entity: Entity, clazz: Class[_]): E = {
    val defaultInstance = createDefaultInstance[E](clazz)
    val fields = clazz.getDeclaredFields.filterNot(_.isSynthetic)
    val idField = fields.head
    idField.setAccessible(true)
    idField.set(defaultInstance, entity.getKey.getNameOrId)
    fields.tail
      .foreach(f => {
        f.setAccessible(true)
        val value = f.getType.getCanonicalName match {
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

  private[datastore] def createDefaultInstance[E](clazz: Class[_]): E = {
    val constructor = clazz.getConstructors.head
    val params = constructor.getParameterTypes.map(cl => cl.getCanonicalName match {
      case ByteClassName => Byte.MinValue
      case IntClassName => Int.MinValue
      case LongClassName => 0L
      case StringClassName => ""
      case FloatClassName => 0.0F
      case DoubleClassName => 0.0
      case BooleanClassName => false
      case JavaUtilDateClassName => new Date(0)
      case DatastoreDateTimeClassName => DateTime.copyFrom(new Date(0))
      case LocalDateTimeClassName => LocalDateTime.MIN
      case ZonedDateTimeClassName => ZonedDateTime.of(1900, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
      case OffsetDateTimeClassName => OffsetDateTime.MIN
      case DatastoreLatLongClassName => LatLng.of(0.0, 0.0)
      case DatastoreBlobClassName => Blob.copyFrom(Array[Byte]())
      case fieldName => throw UnsupportedFieldTypeException(fieldName)
    }).map(_.asInstanceOf[Object])
    constructor.newInstance(params: _*).asInstanceOf[E]
  }
}
