package models

import org.joda.time.DateTime
import org.squeryl.{KeyedEntity, Schema}
import system.DbDef._


class Entity extends KeyedEntity[Int] {
  val id: Int = 0
}

case class Project(var name: String, var code: String, var description: String) extends Entity

case class Info(var projectId: Int, var parentInfoId: Option[Int], var name: String, var keywords: String, var text: String, var childrenCount: Int = 0) extends Entity {
  var lastModified = DateTime.now()

  def this() = this(0, Some(0), "", "", "")

  lazy val children = AppDB.infoToParent.left(this)
  lazy val parent = AppDB.infoToParent.right(this)
  lazy val project = AppDB.projectToInfo.right(this)
  lazy val images = AppDB.infoToInfoImage.left(this)
}

case class InfoImage(infoId: Int, data: Array[Byte], contentType: String) extends KeyedEntity[Long] {
  val id: Long = (Math.random() * Long.MaxValue).toLong
}

case class InfoRevision(infoId: Int, projectId: Int, parentInfoId: Option[Int], name: String, keywords: String, text: String) extends Entity {
  val revisionDate = DateTime.now()
}

object AppDB extends Schema {
  val projectTable = table[Project]
  val infoTable = table[Info]
  val infoImageTable = table[InfoImage]
  val infoRevisionTable = table[InfoRevision]

  on(infoImageTable)(t => declare(
    t.id is primaryKey,
    t.infoId is indexed
  ))
  on(infoRevisionTable)(t => declare(
    t.infoId is indexed
  ))

  val infoToInfoImage = oneToManyRelation(infoTable, infoImageTable) via { (info, image) => info.id === image.infoId}
  val projectToInfo = oneToManyRelation(projectTable, infoTable) via { (project, info) => info.projectId === project.id}
  val infoToParent = oneToManyRelation(infoTable, infoTable) via { (parent, info) => info.parentInfoId === parent.id}
  val infoToRevisions = oneToManyRelation(infoTable, infoRevisionTable) via { (info, rev) => rev.infoId === info.id}

  override def defaultLengthOfString: Int = -1

  override def columnNameFromPropertyName(propertyName: String): String = NamingConventionTransforms.snakify(propertyName)

  override def tableNameFromClassName(tableName: String): String = NamingConventionTransforms.snakify(tableName)
}