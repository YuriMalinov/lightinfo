package system

import play.api.data.{Forms, Mapping, FormError}
import play.api.data.format.Formatter

class EnumerationMapping[T <: Enumeration](enum: T) extends Formatter[T#Value] {
  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], T#Value] = {
    data.get(key) match {
      case Some(value) ⇒ enum.values.find(_.toString == value) match {
        case Some(result) ⇒ Right(result.asInstanceOf[T#Value])
        case None ⇒ error(key, s"$value is not a valid member of enum ${enum.getClass.getSimpleName}")
      }
      case None ⇒ error(key, "Value not provided")
    }
  }


  override def unbind(key: String, value: T#Value): Map[String, String] = Map(key → value.toString)

  private def error(key: String, msg: String) = Left(List(new FormError(key, msg)))
  
  def mapping: Mapping[T#Value] = Forms.of[T#Value](this)
}
