package io.applicative.datastore.util.reflection

case class Field[T >: Any](name: String, value: T)
