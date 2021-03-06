package system

import java.sql.Timestamp

import controllers.NotFoundEx
import models.Page
import org.joda.time.DateTime
import org.squeryl.{KeyedEntityDef, Table, Query, PrimitiveTypeMode}
import org.squeryl.dsl._

object DbDef extends PrimitiveTypeMode {
  implicit val jodaTimeTEF = new NonPrimitiveJdbcMapper[Timestamp, DateTime, TTimestamp](timestampTEF, this) {
    def convertFromJdbc(t: Timestamp) = new DateTime(t)

    def convertToJdbc(t: DateTime) = new Timestamp(t.getMillis)
  }

  /**
   * We define this one here to allow working with Option of our new type, this allso
   * allows the 'nvl' function to work
   */
  implicit val optionJodaTimeTEF = new TypedExpressionFactory[Option[DateTime], TOptionTimestamp]
    with DeOptionizer[Timestamp, DateTime, TTimestamp, Option[DateTime], TOptionTimestamp] {

    val deOptionizer = jodaTimeTEF
  }

  implicit def jodaTimeToTE(s: DateTime): TypedExpression[DateTime, TTimestamp] = jodaTimeTEF.create(s)

  implicit def optionJodaTimeToTE(s: Option[DateTime]): TypedExpression[Option[DateTime], TOptionTimestamp] = optionJodaTimeTEF.create(s)

  def selectPage[T](q: Query[T], page: Int, pageSize: Int) =
    Page(q.page(page * pageSize, pageSize).toSeq, page, pageSize, from(q)(l ⇒ compute(count)).head.measures)

  implicit class Lookup404[T, K](t: Table[T])(implicit ked: KeyedEntityDef[T,K]) {
    def lookup404(k: K): T = t.lookup(k).getOrElse(throw NotFoundEx(s"Can't find key [$k] in table [${t.prefixedName}]"))
  }
}

