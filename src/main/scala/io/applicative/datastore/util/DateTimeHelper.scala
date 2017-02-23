package io.applicative.datastore.util

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

import com.google.cloud.datastore.DateTime

trait DateTimeHelper {

  private val localDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
  private val zonedDateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
  private val offsetDateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def toMilliSeconds(date: Date): Long = {
    date.getTime
  }

  def toMilliSeconds(dateTime: DateTime): Long = {
    dateTime.getTimestampMillis
  }

  def toJavaUtilDate(milliSeconds: Long): Date = {
    new Date(milliSeconds)
  }

  def toDatastoreDateTime(milliSeconds: Long): DateTime = {
    DateTime.copyFrom(toJavaUtilDate(milliSeconds))
  }

  def formatLocalDateTime(localDateTime: LocalDateTime): String = {
    localDateTimeFormatter.format(localDateTime)
  }

  def formatZonedDateTime(zonedDateTime: ZonedDateTime): String = {
    zonedDateTimeFormatter.format(zonedDateTime)
  }

  def formatOffsetDateTime(offsetDateTime: OffsetDateTime): String = {
    offsetDateTimeFormatter.format(offsetDateTime)
  }

  def parseLocalDateTime(timeString: String): LocalDateTime = {
    LocalDateTime.parse(timeString, localDateTimeFormatter)
  }

  def parseZonedDateTime(timeString: String): ZonedDateTime = {
    ZonedDateTime.parse(timeString, zonedDateTimeFormatter)
  }

  def parseOffsetDateTime(timeString: String): OffsetDateTime = {
    OffsetDateTime.parse(timeString, offsetDateTimeFormatter)
  }
}
