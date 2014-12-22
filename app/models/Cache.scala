package models

import org.squeryl.Table

import scala.collection.mutable

/**
 * TODO: Забыл описание класса сделать, да?
 */
class Cache {
  private[this] val data = new mutable.HashMap[Any, Any]()

  def put(key: Any, value: Any): Unit = {
    data.put(key, value)
  }

  def get[T](key: Any): Option[T] = data.get(key).asInstanceOf[Option[T]]

  def getOrElseUpdate[T](key: Any, update: ⇒ T) = data.get(key) match {
    case Some(r) ⇒ r
    case None ⇒
      val v = update
      data.put(key, v)
      v
  }
}

class CachedModel[T, K](table: Table[T]){
//  def get(key: K)(implicit cache: Cache) = {
//    val map = cache.getOrElseUpdate[mutable.HashMap[K, T]](table.prefixedName, new mutable.HashMap)
//    map.get(key) match {
//      case Some(result) ⇒ result
//      case None ⇒
//
//    }
//  }
}