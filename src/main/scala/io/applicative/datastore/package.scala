package io.applicative.datastore

import java.util.Date

import com.google.cloud.datastore.EntityQuery.Builder
import com.google.cloud.datastore.{Blob, DateTime}
import com.google.cloud.datastore.StructuredQuery.{CompositeFilter, PropertyFilter}
import io.applicative.datastore.exception.UnsupportedFieldTypeException
import io.applicative.datastore.util.DateTimeHelper

import scala.reflect.runtime.universe._

package object query {

  def select[E <: BaseEntity : TypeTag] = {
    val builder = new Builder
    SelectClause[E](builder)
  }

  implicit def anyToProperty(a: Any): Property = Property(a)


  sealed abstract class Clause[E <: BaseEntity : TypeTag]() {
    protected val builder: Builder
    protected var filters: List[PropertyFilter]
    def asList: List[E] = {
      val query = builder.build()
      DatastoreService.runQueryForList[E](query)
    }

    def asSingle: Option[E] = {
      filters match {
        case head :: tail => builder.setFilter(CompositeFilter.and(head, tail: _*))
        case Nil => // do nothing
      }
      val query = builder.build()
      DatastoreService.runQueryForSingleOpt[E](query)
    }
  }


  case class SelectClause[T <: BaseEntity : TypeTag] (override protected val builder: Builder) extends Clause[T] {
    override protected var filters: List[PropertyFilter] = List()

    def where(filter: PropertyFilter): AndSelectClause[T] = {
      AndSelectClause(builder, List(filter))
    }
  }

  case class AndSelectClause[T <: BaseEntity : TypeTag](
                                                        override protected val builder: Builder,
                                                        override protected val filters: List[PropertyFilter]
                                                       ) extends SelectClause(builder) {
    def and(filter: PropertyFilter) = {
      AndSelectClause(builder, filters :+ filter)
    }
  }

  case class PropertyNameValue(name: Any, value: Any)

  case class Property(value: Any) extends DateTimeHelper {

    def |<|(another: Property): PropertyFilter = {
      (value, another.value) match {
        case (n: String, v: Boolean) => PropertyFilter.lt(n, v)
        case (n: String, v: Byte) => PropertyFilter.lt(n, v)
        case (n: String, v: Int) => PropertyFilter.lt(n, v)
        case (n: String, v: Long) => PropertyFilter.lt(n, v)
        case (n: String, v: Float) => PropertyFilter.lt(n, v)
        case (n: String, v: Double) => PropertyFilter.lt(n, v)
        case (n: String, v: String) => PropertyFilter.lt(n, v)
        case (n: String, v: Date) => PropertyFilter.lt(n, toMilliSeconds(v))
        case (n: String, v: DateTime) => PropertyFilter.lt(n, v)
        case (n: String, v: Blob) => PropertyFilter.lt(n, v)
        case (n, v) => throw UnsupportedFieldTypeException(v.getClass.getCanonicalName)
      }
    }

    def |>|(another: Property) = {
      (value, another.value) match {
        case (n: String, v: Boolean) => PropertyFilter.gt(n, v)
        case (n: String, v: Byte) => PropertyFilter.gt(n, v)
        case (n: String, v: Int) => PropertyFilter.gt(n, v)
        case (n: String, v: Long) => PropertyFilter.gt(n, v)
        case (n: String, v: Float) => PropertyFilter.gt(n, v)
        case (n: String, v: Double) => PropertyFilter.gt(n, v)
        case (n: String, v: String) => PropertyFilter.gt(n, v)
        case (n: String, v: Date) => PropertyFilter.gt(n, toMilliSeconds(v))
        case (n: String, v: DateTime) => PropertyFilter.gt(n, v)
        case (n: String, v: Blob) => PropertyFilter.gt(n, v)
        case (n, v) => throw UnsupportedFieldTypeException(v.getClass.getCanonicalName)
      }
    }

    def |<=|(another: Property) = {
      (value, another.value) match {
        case (n: String, v: Boolean) => PropertyFilter.le(n, v)
        case (n: String, v: Byte) => PropertyFilter.le(n, v)
        case (n: String, v: Int) => PropertyFilter.le(n, v)
        case (n: String, v: Long) => PropertyFilter.le(n, v)
        case (n: String, v: Float) => PropertyFilter.le(n, v)
        case (n: String, v: Double) => PropertyFilter.le(n, v)
        case (n: String, v: String) => PropertyFilter.le(n, v)
        case (n: String, v: Date) => PropertyFilter.le(n, toMilliSeconds(v))
        case (n: String, v: DateTime) => PropertyFilter.le(n, v)
        case (n: String, v: Blob) => PropertyFilter.le(n, v)
        case (n, v) => throw UnsupportedFieldTypeException(v.getClass.getCanonicalName)
      }
    }

    def |>=|(another: Property) = {
      (value, another.value) match {
        case (n: String, v: Boolean) => PropertyFilter.ge(n, v)
        case (n: String, v: Byte) => PropertyFilter.ge(n, v)
        case (n: String, v: Int) => PropertyFilter.ge(n, v)
        case (n: String, v: Long) => PropertyFilter.ge(n, v)
        case (n: String, v: Float) => PropertyFilter.ge(n, v)
        case (n: String, v: Double) => PropertyFilter.ge(n, v)
        case (n: String, v: String) => PropertyFilter.ge(n, v)
        case (n: String, v: Date) => PropertyFilter.ge(n, toMilliSeconds(v))
        case (n: String, v: DateTime) => PropertyFilter.ge(n, v)
        case (n: String, v: Blob) => PropertyFilter.ge(n, v)
        case (n, v) => throw UnsupportedFieldTypeException(v.getClass.getCanonicalName)
      }
    }

    def |==|(another: Property) = {
      (value, another.value) match {
        case (n: String, v: Boolean) => PropertyFilter.eq(n, v)
        case (n: String, v: Byte) => PropertyFilter.eq(n, v)
        case (n: String, v: Int) => PropertyFilter.eq(n, v)
        case (n: String, v: Long) => PropertyFilter.eq(n, v)
        case (n: String, v: Float) => PropertyFilter.eq(n, v)
        case (n: String, v: Double) => PropertyFilter.eq(n, v)
        case (n: String, v: String) => PropertyFilter.eq(n, v)
        case (n: String, v: Date) => PropertyFilter.eq(n, toMilliSeconds(v))
        case (n: String, v: DateTime) => PropertyFilter.eq(n, v)
        case (n: String, v: Blob) => PropertyFilter.eq(n, v)
        case (n, v) => throw UnsupportedFieldTypeException(v.getClass.getCanonicalName)
      }
    }
  }
}
