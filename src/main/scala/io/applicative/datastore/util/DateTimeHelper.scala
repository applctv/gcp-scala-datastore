package io.applicative.datastore.util

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

import com.google.cloud.datastore.DateTime

private[datastore] trait DateTimeHelper {

  private val localDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  private val zonedDateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
  private val offsetDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  private[datastore] def toMilliSeconds(date: Date): Long = {
    date.getTime
  }

  private[datastore] def toMilliSeconds(dateTime: DateTime): Long = {
    dateTime.getTimestampMillis
  }

  private[datastore] def toJavaUtilDate(milliSeconds: Long): Date = {
    new Date(milliSeconds)
  }

  private[datastore] def toDatastoreDateTime(milliSeconds: Long): DateTime = {
    DateTime.copyFrom(toJavaUtilDate(milliSeconds))
  }

  private[datastore] def formatLocalDateTime(localDateTime: LocalDateTime): String = {
    localDateTimeFormatter.format(localDateTime)
  }

  private[datastore] def formatZonedDateTime(zonedDateTime: ZonedDateTime): String = {
    zonedDateTimeFormatter.format(zonedDateTime)
  }

  private[datastore] def formatOffsetDateTime(offsetDateTime: OffsetDateTime): String = {
    offsetDateTimeFormatter.format(offsetDateTime)
  }

  private[datastore] def parseLocalDateTime(timeString: String): LocalDateTime = {
    LocalDateTime.parse(timeString, localDateTimeFormatter)
  }

  private[datastore] def parseZonedDateTime(timeString: String): ZonedDateTime = {
    ZonedDateTime.parse(timeString, zonedDateTimeFormatter)
  }

  private[datastore] def parseOffsetDateTime(timeString: String): OffsetDateTime = {
    OffsetDateTime.parse(timeString, offsetDateTimeFormatter)
  }
}
