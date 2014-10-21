package models

/**
 * Created by Yuri on 18.10.2014.
 */
case class Page[T](list: Seq[T], page: Int, pageSize: Int, totalCount: Long)
