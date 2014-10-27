package models

import org.joda.time.DateTime
import org.squeryl.{KeyedEntity, Schema}
import securesocial.core._
import securesocial.core.providers.Token
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

  override def defaultLengthOfString: Int = -1

  override def columnNameFromPropertyName(propertyName: String): String = NamingConventionTransforms.snakify(propertyName)

  override def tableNameFromClassName(tableName: String): String = NamingConventionTransforms.snakify(tableName)
}