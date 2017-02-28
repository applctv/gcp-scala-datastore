package io.applicative.datastore.exception

case class MissedEmptyConstructorException(className: String) extends
  NoSuchMethodException(s"Can not find empty constructor for class $className") {

}
