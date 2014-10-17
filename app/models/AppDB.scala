package models

import org.joda.time.DateTime
import org.squeryl.{PrimitiveTypeMode, Schema, KeyedEntity}

import system.DbDef._


class Entity extends KeyedEntity[Int] {
  val id: Int = 0
}

case class Project(var name: String, var description: String) extends Entity

case class Info(var projectId: Int, var parentInfoId: Option[Int], var name: String, var text: String) extends Entity {
  var lastModified = DateTime.now()

  def this() = this(0, Some(0), "", "")

  lazy val children = AppDB.infoToParent.left(this)
  lazy val parent = AppDB.infoToParent.right(this)
  lazy val project = AppDB.projectToInfo.right(this)
  lazy val images = AppDB.infoToInfoImage.left(this)
}

case class InfoImage(infoId: Int, data: Array[Byte]) extends Entity

case class InfoRevision(infoId: Int, projectId: Int, parentInfoId: Option[Int], name: String, text: String) extends Entity {
  val revisionDate = DateTime.now()
}

object AppDB extends Schema {
  val projectTable = table[Project]
  val infoTable = table[Info]
  val infoImageTable = table[InfoImage]
  val infoRevisionTable = table[InfoRevision]

  on(infoImageTable)(t => declare(
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