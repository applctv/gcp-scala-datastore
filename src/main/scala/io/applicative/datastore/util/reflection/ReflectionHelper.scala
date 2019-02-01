package io.applicative.datastore.util.reflection

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

import com.google.cloud.Timestamp
import com.google.cloud.datastore._
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
  private val KeyClassName = getClassName[Key]()

  private[datastore] def extractRuntimeClass[E: ClassTag](): RuntimeClass = {
    val runtimeClass = classTag[E].runtimeClass
    if (runtimeClass == classOf[Nothing]) {
      throw MissedTypeParameterException()
    }
    runtimeClass
  }

  private[datastore] def instanceToDatastoreEntity[E: TypeTag : ClassTag](key: Key, classInstance: E, clazz: Class[_])(implicit tt: TypeTag[E]): Entity = {
    var builder = Entity.newBuilder(key.key)
    val primaryConstructor = clazz.getConstructors.head
    var paramCounter = -1
    val annotates = primaryConstructor
      .getParameters
      .tail
      .map { p =>
        paramCounter += 1
        (paramCounter, p.isAnnotationPresent(classOf[excludeFromIndexes]))
      }
      .toMap
    paramCounter = -1
    clazz.getDeclaredFields
      .filterNot(_.isSynthetic)
      // Take all fields except the first one assuming it is an ID field, which is already encapsulated in the Key
      .tail
      .map(f => {
        paramCounter += 1
        val excludeFromIndexes = annotates(paramCounter)
        f.setAccessible(true)
        Field(f.getName, f.get(classInstance), excludeFromIndexes)
      })
      .foreach {
        case Field(name, Some(value: Any), excludeFromIndexes) =>
          builder = setValue(Field(name, value, excludeFromIndexes), builder)
        case Field(name, None, _) =>
          builder.setNull(name)
        case field => builder =
          setValue(field, builder)
      }
    builder.build()
  }

  private def setValue(field: Field[_], builder: Entity.Builder) = {
    def setLongValue(name: String, value: Long, excludeFromIndexes: Boolean) = {
      val longValue = LongValue
        .newBuilder(value)
        .setExcludeFromIndexes(excludeFromIndexes)
        .build()
      builder.set(name, longValue)
    }

    def setDoubleValue(name: String, value: Double, excludeFromIndexes: Boolean) = {
      val doubleValue = DoubleValue
        .newBuilder(value)
        .setExcludeFromIndexes(excludeFromIndexes)
        .build()
      builder.set(name, doubleValue)
    }

    def setStringValue(name: String, value: String, excludeFromIndexes: Boolean) = {
      val stringValue = StringValue
        .newBuilder(value)
        .setExcludeFromIndexes(excludeFromIndexes)
        .build()
      builder.set(name, stringValue)
    }

    field match {
      case Field(name, value: Boolean, excludeFromIndexes) =>
        val booleanValue = BooleanValue.newBuilder(value).setExcludeFromIndexes(excludeFromIndexes).build()
        builder.set(name, booleanValue)
      case Field(name, value: Byte, excludeFromIndexes) =>
        setLongValue(name, value, excludeFromIndexes)
      case Field(name, value: Int, excludeFromIndexes) =>
        setLongValue(name, value, excludeFromIndexes)
      case Field(name, value: Long, excludeFromIndexes) =>
        setLongValue(name, value, excludeFromIndexes)
      case Field(name, value: Float, excludeFromIndexes) =>
        setDoubleValue(name, value, excludeFromIndexes)
      case Field(name, value: Double, excludeFromIndexes) =>
        setDoubleValue(name, value, excludeFromIndexes)
      case Field(name, value: String, excludeFromIndexes) =>
        setStringValue(name, value, excludeFromIndexes)
      case Field(name, value: Date, excludeFromIndexes) =>
        setLongValue(name, toMilliSeconds(value), excludeFromIndexes)
      case Field(name, value: Timestamp, excludeFromIndexes) =>
        val tsValue = TimestampValue
          .newBuilder(value)
          .setExcludeFromIndexes(excludeFromIndexes)
          .build()
        builder.set(name, tsValue)
      case Field(name, value: LocalDateTime, excludeFromIndexes) =>
        //TODO: come up with better way to store the objects from the java.time package.
        // Current implementation does not allow to use filtering for the objects of these types.
        setStringValue(name, formatLocalDateTime(value), excludeFromIndexes)
      case Field(name, value: OffsetDateTime, excludeFromIndexes) =>
        setStringValue(name, formatOffsetDateTime(value), excludeFromIndexes)
      case Field(name, value: ZonedDateTime, excludeFromIndexes) =>
        setStringValue(name, formatZonedDateTime(value), excludeFromIndexes)
      case Field(name, value: LatLng, excludeFromIndexes) =>
        val latLngValue = LatLngValue
          .newBuilder(value)
          .setExcludeFromIndexes(excludeFromIndexes)
          .build()
        builder.set(name, latLngValue)
      case Field(name, value: Blob, excludeFromIndexes) =>
        val blobValue = BlobValue
          .newBuilder(value)
          .setExcludeFromIndexes(excludeFromIndexes)
          .build()
        builder.set(name, blobValue)
      case Field(name, value: Serializable, excludeFromIndexes) =>
        val blob = Blob.copyFrom(objectToBytes(value))
        val blobValue = BlobValue
          .newBuilder(blob)
          .setExcludeFromIndexes(excludeFromIndexes)
          .build()
        builder.set(name, blobValue)
      case Field(_, value, _) => throw UnsupportedFieldTypeException(value.getClass.getCanonicalName)
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
          if (fieldClassName == KeyClassName) {
            field.set(Key(entity.getKey))
          } else {
            field.set(entity.getKey.getNameOrId)
          }
        } else {
          val value = fieldClassName match {
            case OptionClassName =>
              if (!entity.contains(fieldName) || entity.isNull(fieldName)) {
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
      case other => bytesToObject[Serializable](entity.getBlob(fieldName).toByteArray)
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
      case _ => null
    }).map(_.asInstanceOf[Object])
    constructor.newInstance(params: _*).asInstanceOf[E]
  }

  protected def getClassName(clazz: Class[_]): String = {
    runtimeMirror(clazz.getClassLoader).classSymbol(clazz).fullName
  }

  protected def getClassName[E : TypeTag](): String = {
    typeOf[E].typeSymbol.fullName
  }

  private def objectToBytes[T <: Serializable](o: T) = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(o)
    oos.close()
    stream.toByteArray
  }

  private def bytesToObject[T <: Serializable](bytes: Array[Byte]): T = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject
    ois.close()
    value.asInstanceOf[T]
  }
}
