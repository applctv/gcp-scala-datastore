package io.applicative.datastore.util.reflection

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

import com.google.cloud.Timestamp
import com.google.cloud.datastore.{Blob, Entity, LatLng}
import io.applicative.datastore.Key
import io.applicative.datastore.exception.{MissedTypeParameterException, UnsupportedFieldTypeException}
import io.applicative.datastore.util.DateTimeHelper

import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.reflect.runtime.universe._

private[datastore] trait ReflectionHelper extends DateTimeHelper {

  private val ByteClassName = getClassName[Byte]()
  private val IntClassName = getClassName[Int]()
  private val LongClassName = getClassName[Long]()
  private val FloatClassName = getClassName[Float]()
  private val DoubleClassName = getClassName[Double]()
  private val StringClassName = getClassName[String]()
  private val JavaUtilDateClassName = getClassName[Date]()
  private val BooleanClassName = getClassName[Boolean]()
  private val DatastoreTimestampClassName = getClassName[Timestamp]()
  private val DatastoreLatLongClassName = getClassName[LatLng]()
  private val DatastoreBlobClassName = getClassName[Blob]()
  private val LocalDateTimeClassName = getClassName[LocalDateTime]()
  private val ZonedDateTimeClassName = getClassName[ZonedDateTime]()
  private val OffsetDateTimeClassName = getClassName[OffsetDateTime]()
  private val OptionClassName = getClassName[Option[_]]()

  private[datastore] def extractRuntimeClass[E: ClassTag](): RuntimeClass = {
    val runtimeClass = classTag[E].runtimeClass
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
        case Field(name, Some(value: Any)) => builder = setValue(Field(name, value), builder)
        case Field(name, None) => builder.setNull(name)
        case field => builder = setValue(field, builder)
      }
    builder.build()
  }

  private def setValue(field: Field[_], builder: Entity.Builder) = {
    field match {
      case Field(name, value: Boolean) => builder.set(name, value)
      case Field(name, value: Byte) => builder.set(name, value)
      case Field(name, value: Int) => builder.set(name, value)
      case Field(name, value: Long) => builder.set(name, value)
      case Field(name, value: Float) => builder.set(name, value)
      case Field(name, value: Double) => builder.set(name, value)
      case Field(name, value: String) => builder.set(name, value)
      case Field(name, value: Date) => builder.set(name, toMilliSeconds(value))
      case Field(name, value: Timestamp) => builder.set(name, value)
      case Field(name, value: LocalDateTime) => builder.set(name, formatLocalDateTime(value))
      case Field(name, value: OffsetDateTime) => builder.set(name, formatOffsetDateTime(value))
      case Field(name, value: ZonedDateTime) => builder.set(name, formatZonedDateTime(value))
      case Field(name, value: LatLng) => builder.set(name, value)
      case Field(name, value: Blob) => builder.set(name, value)
      case Field(name, value) => throw UnsupportedFieldTypeException(value.getClass.getCanonicalName)
    }
  }

  private[datastore] def datastoreEntityToInstance[E : TypeTag : ClassTag](entity: Entity, clazz: Class[_]): E = {
    val defaultInstance = createDefaultInstance[E](clazz)
    setActualFieldValues(defaultInstance, entity)
    defaultInstance.asInstanceOf[E]
  }

  private def setActualFieldValues[T](a: T, entity: Entity)(implicit tt: TypeTag[T], ct: ClassTag[T]): Unit = {
    tt.tpe.members.collect {
      case m if m.isMethod && m.asMethod.isCaseAccessor => m.asMethod
    } foreach { member => {
        val field = tt.mirror.reflect(a).reflectField(member)
        val fieldClassName = member.returnType.typeSymbol.fullName
        val fieldName = member.name.toString
        if (fieldName == "id") {
          field.set(entity.getKey.getNameOrId)
        } else {
          val value = fieldClassName match {
            case OptionClassName =>
              if (entity.isNull(fieldName)) {
                None
              } else {
                val genericClassName = member.returnType.typeArgs.head.typeSymbol.fullName
                Some(getValue(genericClassName, fieldName, entity))
              }
            case className =>
              getValue(className, fieldName, entity)
          }
          field.set(value)
        }
      }
    }
  }

  private def getValue(className: String, fieldName: String, entity: Entity): Any = {
    className match {
      case ByteClassName => entity.getLong(fieldName).toByte
      case IntClassName => entity.getLong(fieldName).toInt
      case LongClassName => entity.getLong(fieldName)
      case StringClassName => entity.getString(fieldName)
      case FloatClassName => entity.getDouble(fieldName).toFloat
      case DoubleClassName => entity.getDouble(fieldName)
      case BooleanClassName => entity.getBoolean(fieldName)
      case JavaUtilDateClassName => toJavaUtilDate(entity.getLong(fieldName))
      case DatastoreTimestampClassName => entity.getTimestamp(fieldName)
      case LocalDateTimeClassName => parseLocalDateTime(entity.getString(fieldName))
      case ZonedDateTimeClassName => parseZonedDateTime(entity.getString(fieldName))
      case OffsetDateTimeClassName => parseOffsetDateTime(entity.getString(fieldName))
      case DatastoreLatLongClassName => entity.getLatLng(fieldName)
      case DatastoreBlobClassName => entity.getBlob(fieldName)
      case otherClassName => throw UnsupportedFieldTypeException(otherClassName)
    }
  }

  private[datastore] def createDefaultInstance[E](clazz: Class[_]): E = {
    val constructor = clazz.getConstructors.head
    val params = constructor.getParameterTypes.map(cl => getClassName(cl) match {
      case ByteClassName => Byte.MinValue
      case IntClassName => Int.MinValue
      case LongClassName => 0L
      case StringClassName => ""
      case FloatClassName => 0.0F
      case DoubleClassName => 0.0
      case BooleanClassName => false
      case JavaUtilDateClassName => new Date(0)
      case DatastoreTimestampClassName => Timestamp.of(new Date(0))
      case LocalDateTimeClassName => LocalDateTime.MIN
      case ZonedDateTimeClassName => ZonedDateTime.of(1900, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC)
      case OffsetDateTimeClassName => OffsetDateTime.MIN
      case DatastoreLatLongClassName => LatLng.of(0.0, 0.0)
      case DatastoreBlobClassName => Blob.copyFrom(Array[Byte]())
      case OptionClassName => None
      case fieldName => throw UnsupportedFieldTypeException(fieldName)
    }).map(_.asInstanceOf[Object])
    constructor.newInstance(params: _*).asInstanceOf[E]
  }

  private[datastore] def getClassName(clazz: Class[_]): String = {
    runtimeMirror(clazz.getClassLoader).classSymbol(clazz).fullName
  }

  private[datastore] def getClassName[E : TypeTag](): String = {
    typeOf[E].typeSymbol.fullName
  }
}
