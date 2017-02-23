package io.applicative.datastore.exception

case class UnsupportedFieldTypeException(fieldTypeName: String)
  extends RuntimeException(s"Fields of type $fieldTypeName are not supported")
