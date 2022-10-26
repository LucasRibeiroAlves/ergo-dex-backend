package org.ergoplatform.common.sql

import doobie.Update
import doobie.util.Write
import doobie.util.log.LogHandler

/** Database table access operations layer.
  */
trait QuerySet[T] {

  /** Name of the table according to a database schema.
    */
  val tableName: String

  /** Table column names listing according to a database schema.
    */
  val fields: List[String]

  final def insert(implicit lh: LogHandler, w: Write[T]): Update[T] =
    Update[T](s"insert into $tableName ($fieldsString) values ($holdersString)")

  final def insertNoConflict(implicit lh: LogHandler, w: Write[T]): Update[T] =
    Update[T](s"insert into $tableName ($fieldsString) values ($holdersString) on conflict do nothing")

  def fieldsString: String =
    fields.mkString(", ")

  private def holdersString: String =
    fields.map(_ => "?").mkString(", ")
}
