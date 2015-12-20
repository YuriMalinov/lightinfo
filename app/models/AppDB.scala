package models

import controllers.NotFoundEx
import org.joda.time.DateTime
import org.squeryl.annotations.Transient
import org.squeryl.{KeyedEntity, KeyedEntityDef, Schema, Table}
import securesocial.core._
import securesocial.core.providers.Token
import system.DbDef._
import system.EnumerationMapping


class Entity extends KeyedEntity[Int] {
  val id: Int = 0
}

object ProjectType extends Enumeration {
  val Public = Value(1, "Open")
  val Protected = Value(2, "Protected")
  val Private = Value(3, "Private")

  def isClosed(v: Value) = v == this.Protected || v == this.Private
}

object ProjectTypeMapping extends EnumerationMapping(ProjectType)

case class Project(var name: String, var code: String, var description: String, var projectType: ProjectType.Value, var allowRequestForAccess: Boolean) extends Entity {
  def this() = this("", "", "", ProjectType.Public, true)
}

class SimpleModel[K, T <: KeyedEntity[K]](table: Table[T])(implicit ked: KeyedEntityDef[T,K]) {
  def get(key: K) = table.lookup(key).getOrElse(throw NotFoundEx(s"Can't find [${table.prefixedName}] with key [$key]"))
}

object Project extends SimpleModel[Int, Project](AppDB.projectTable) {
  def findByCode(code: String): Project = AppDB.projectTable.where(p ⇒ p.code === code).headOption.getOrElse(throw NotFoundEx(s"Can't find project with code [$code]"))
}

case class Info(var projectId: Int, var parentInfoId: Option[Int], var name: String, var keywords: String, var code: Option[String], var text: String, var childrenCount: Int = 0, var isPrivate: Boolean = false, var trash: Boolean = false) extends Entity {
  var lastModified = DateTime.now()

  @Transient
  lazy val projectCode = if (projectId == 0) "-1" else project.single.code

  def this() = this(0, Some(0), "", "", Some(""), "")

  def project = AppDB.projectToInfo.right(this)

  def makeRevision(userId: Int) = InfoRevision(
    infoId = id,
    projectId = projectId,
    parentInfoId = parentInfoId,
    name = name,
    keywords = keywords,
    code = code,
    isPrivate = isPrivate,
    text = text,
    userId = userId
  )

  def restoreRevision(revision: InfoRevision): Unit = {
    name = revision.name
    keywords = revision.keywords
    code = revision.code
    text = revision.text
    parentInfoId = revision.parentInfoId
    isPrivate = revision.isPrivate
    projectId = revision.projectId
  }
}

case class InfoDisplay(id: Int, name: String, lineCount: Int, code: Option[String], keywords: String, level: Int, childrenCount: Int, trash: Boolean, isPrivate: Boolean)

object Info {
  def findByProject(projectId: Int) = from(AppDB.infoTable)(i ⇒ where(i.projectId === projectId) select i orderBy i.name).toSeq


  def sortByParent(infos: Seq[Info]): Seq[InfoDisplay] = {
    val data = infos.groupBy(_.parentInfoId.getOrElse(0))

    def subList(info: Info, level: Int, noRecursion: Set[Int]): List[InfoDisplay] = {
      val children = data.getOrElse(info.id, Nil)
      val subItems = children.flatMap(i ⇒ if (noRecursion.contains(info.id)) Nil else subList(i, level + 1, noRecursion + info.id))

      List(InfoDisplay(info.id, info.name, info.text.count(_ == '\n'), info.code, info.keywords, level, subItems.size, info.trash, info.isPrivate)) ++ subItems
    }

    data.getOrElse(0, Nil).flatMap(i ⇒ subList(i, 0, Set()))
  }
}

case class InfoImage(infoId: Int, data: Array[Byte], contentType: String) extends KeyedEntity[Long] {
  val id: Long = (Math.random() * Long.MaxValue).toLong
}

case class InfoRevision(infoId: Int, projectId: Int, parentInfoId: Option[Int], name: String, keywords: String, var code: Option[String], var isPrivate: Boolean, text: String, userId: Int) extends Entity {
  def this() = this(0, 0, Option(0), "", "", Some(""), false, "", 0)

  def info = AppDB.infoToRevisions.right(this)

  @Transient
  lazy val infoCached = info.single

  def user = AppDB.userToRevisions.right(this)

  val revisionDate = DateTime.now()
}


case class User(override val id: Int, var providerId: String, var providerUserId: String, var email: Option[String],
                var firstName: String, var lastName: String, var avatarUrl: Option[String],
                var hasher: Option[String], var password: Option[String], var salt: Option[String]) extends Entity with Identity {

  override def fullName = firstName + " " + lastName

  override def identityId = IdentityId(providerUserId, providerId)

  override def oAuth1Info = None

  override def oAuth2Info = None

  override def passwordInfo = hasher match {
    case None ⇒ None
    case Some(hash) ⇒ Some(PasswordInfo(hash, password.get, salt))
  }

  override def authMethod = AuthenticationMethod.UserPassword
}

object UserStatus extends Enumeration {
  val Request = Value(1, "Request")
  val Active = Value(2, "Active")
  val Admin = Value(3, "Admin")
  val Blocked = Value(4, "Blocked")
}

case class UserInProject(userId: Int, projectId: Int, var userStatus: UserStatus.Value) extends Entity {
  def this() = this(0, 0, UserStatus.Request)
}

class AuthenticatorHolder(auth: Authenticator) extends KeyedEntity[String] {
  def this() = this(Authenticator("", IdentityId("", ""), DateTime.now(), DateTime.now(), DateTime.now()))

  val id = auth.id
  val creationDate = auth.creationDate
  val lastUsed = auth.lastUsed
  val expirationDate = auth.expirationDate
  val providerId = auth.identityId.providerId
  val providerUserId = auth.identityId.userId

  def toAuthenticator = Authenticator(id, IdentityId(providerUserId, providerId), creationDate, lastUsed, expirationDate)
}

object AppDB extends Schema {
  val projectTable = table[Project]
  val infoTable = table[Info]
  val infoImageTable = table[InfoImage]
  val infoRevisionTable = table[InfoRevision]

  on(infoTable)(t => declare(
    columns(t.projectId, t.code) are unique
  ))
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


  // SecureSocial & company
  val userTable = table[User]("app_user")
  on(userTable)(t ⇒ declare(
    columns(t.providerId, t.providerUserId) are unique
  ))

  val tokenTable = table[Token]
  on(tokenTable)(t ⇒ declare(
    t.uuid is primaryKey
  ))

  val authenticatorTable = table[AuthenticatorHolder]

  val userInProjectTable = table[UserInProject]
  on(userInProjectTable)(t ⇒ declare(
    columns(t.userId, t.projectId) are unique
  ))
  val userInProjectToUser = oneToManyRelation(userTable, userInProjectTable) via { (u, up) ⇒ up.userId === u.id}
  val userInProjectToProject = oneToManyRelation(projectTable, userInProjectTable) via { (p, up) ⇒ up.projectId === p.id}
  val userToRevisions = oneToManyRelation(userTable, infoRevisionTable) via { (u, r) ⇒ r.userId === u.id}

  override def defaultLengthOfString: Int = -1

  override def columnNameFromPropertyName(propertyName: String): String = NamingConventionTransforms.snakify(propertyName)

  override def tableNameFromClassName(tableName: String): String = NamingConventionTransforms.snakify(tableName)
}