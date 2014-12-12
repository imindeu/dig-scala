package dig


import play.api.data._
import play.api.libs.json._
import anorm._
import org.joda.time._
import org.joda.time.format._
import java.math.BigDecimal
import java.sql.{PreparedStatement, Statement}

object AnormExtension {

  /** Cache expiration value for rarely changing values, e.g. ViOrszag lists. */
  val cacheExpirationLong = 3600
  /** Cache expiration value for moderately changing values. */
  val cacheExpirationShort = 300

  implicit def toPk[A](opt: Option[A]): Pk[A] = opt match {
    case Some(id) => Id(id)
    case None => NotAssigned
  }

  /**
   * Defines an anorm primary key form mapping.
   *
   * {{{
   * Form(
   *   "name" -> primaryKey(text)
   * )
   * }}}
   *
   * @param mapping The mapping to make a primary key.
   */
  def primaryKey[A](mapping: Mapping[A]): Mapping[Pk[A]] = OptionalMapping(mapping).transform(toPk[A], _.toOption)

  implicit object JodaDateTimeOrdering extends Ordering[org.joda.time.DateTime] {
    val dtComparer = DateTimeComparator.getInstance()

    def compare(x: DateTime, y: DateTime): Int = {
      dtComparer.compare(x, y)
    }
  }

  def closeQuietly(stmt: Statement) {
    try {
      stmt.close()
    } catch {
      case e: Exception =>
      // do nothing
    }
  }

  val dateFormatGeneration: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSS")

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
      case d: java.sql.Date => Right(new DateTime(d.getTime))
      case str: java.lang.String => Right(dateFormatGeneration.parseDateTime(str))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass) )
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: PreparedStatement, index: Int, aValue: DateTime): Unit = {
      s.setTimestamp(index, new java.sql.Timestamp(aValue.withMillisOfSecond(0).getMillis) )
    }
  }

  def statementSet[A](s: PreparedStatement, idx: Int, value: A)(implicit toStatement: ToStatement[A]): Unit = {
    toStatement.set(s, idx, value)
  }

  implicit def rowToInt: Column[Int] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bd: BigDecimal => Right(bd.intValue())
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Int for column " + qualified))
    }
  }

  implicit def rowToLong: Column[Long] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int: Long)
      case long: Long => Right(long)
      case bd: BigDecimal => Right(bd.longValue())
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Long for column " + qualified))
    }
  }

  implicit def rowToBoolean: Column[Boolean] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bool: Boolean => Right(bool)
      case i: Int => Right(i == 1)
      case bd: BigDecimal => Right(bd.intValue() == 1)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Boolean for column " + qualified))
    }
  }

  implicit val columnToString: Column[String] = Column.nonNull[String] { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case string: String => Right(string)
      case clob: java.sql.Clob => Right(clob.getSubString(1, clob.length.asInstanceOf[Int]))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value: ${value.asInstanceOf[AnyRef].getClass} to String for column $qualified"))
    }
  }


  implicit object PkFormat extends Format[Pk[Long]] {
    def reads(json: JsValue): JsResult[Pk[Long]] = JsSuccess (
      json.asOpt[Long].map(id => Id(id)).getOrElse(NotAssigned)
    )
    def writes(id: Pk[Long]): JsValue = id.map(JsNumber(_)).getOrElse(JsNull)
  }

  class RichSQL(val query: String, val parameterValues: (Any, ParameterValue[Any])*) {
    /**
     * Convert this object into an anorm.SqlQuery
     */
    def toSQL = SQL(query).on(parameterValues: _*)

    /**
     * Similar to anorm.SimpleSql.on, but takes lists instead of single values.
     * Each list is converted into a set of values, and then passed to anorm's
     * on function when toSQL is called.
     */
    def onList[A](args: (String, Iterable[A])*)(implicit toParameterValue: (A) => ParameterValue[A]) = {
      val condensed = args.map {
        case (name, values) =>
          val search = "{" + name + "}"
          val valueNames = values.zipWithIndex.map {
            case (value, index) => name + "_" + index
          }
          val placeholders = valueNames.map {
            name => "{" + name + "}"
          }
          val replace = placeholders.mkString(",")
          val converted = values.map {
            value => toParameterValue(value).asInstanceOf[ParameterValue[Any]]
          }
          val parameters = valueNames.zip(converted)
          (search, replace, parameters)
      }
      val newQuery = condensed.foldLeft(query) {
        case (newQuery, (search, replace, _)) =>
          newQuery.replace(search, replace)
      }
      val newValues = parameterValues ++ condensed.map {
        case (_, _, parameters) => parameters
      }.flatten
      new RichSQL(newQuery, newValues: _*)
    }

    def onTupleList[A, B](args: (String, Iterable[(A, B)])*)
                         (implicit toParameterValueA: (A) => ParameterValue[A], toParameterValueB: (B) => ParameterValue[B]) = {
      val condensed = args.map {
        case (name, values) =>
          val search = "{" + name + "}"
          val valueNames = values.zipWithIndex.map {
            case (value, index) => (name + "_a" + index, name + "_b" + index)
          }
          val placeholders = valueNames.map {
            name => ("{" + name._1 + "}", "{" + name._2 + "}")
          }
          val replace = placeholders.map(p => "(" + p._1 + "," + p._2 + ")").mkString(",")
          val converted = values.flatMap {
            value => List(
              toParameterValue(value._1).asInstanceOf[ParameterValue[Any]],
              toParameterValue(value._2).asInstanceOf[ParameterValue[Any]]
            )
          }
          val parameters = valueNames.flatMap(n => List(n._1, n._2)).zip(converted)
          (search, replace, parameters)
      }
      val newQuery = condensed.foldLeft(query) {
        case (newQuery, (search, replace, _)) =>
          newQuery.replace(search, replace)
      }
      val newValues = parameterValues ++ condensed.map {
        case (_, _, parameters) => parameters
      }.flatten
      new RichSQL(newQuery, newValues: _*)
    }
  }

  object RichSQL {
    def apply[A](query: String) = new RichSQL(query)
  }

}
