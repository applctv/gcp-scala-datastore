package io.applicative.datastore.exception

case class UnsupportedIdTypeException (idTypeName: String) extends
  RuntimeException(s"Fields of type $idTypeName not supported")