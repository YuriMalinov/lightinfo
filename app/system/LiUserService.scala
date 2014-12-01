package system

import controllers.ForbiddenEx
import models.{AppDB, User}
import org.joda.time.DateTime
import play.api.{Play, Application}
import securesocial.core.providers.Token
import securesocial.core.{Identity, IdentityId, UserServicePlugin}
import system.DbDef._

class LiUserService(app: Application) extends UserServicePlugin(app) {
  override def find(id: IdentityId): Option[Identity] = inTransaction {
    from(AppDB.userTable)(u ⇒ where(u.providerId === id.providerId and u.providerUserId === id.userId) select u).singleOption
  }

  override def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = inTransaction {
    from(AppDB.userTable)(u ⇒ where(u.providerId === providerId and u.email === Some(email)) select u).singleOption
  }

  override def deleteToken(uuid: String): Unit = inTransaction {
    AppDB.tokenTable.delete(from(AppDB.tokenTable)(t ⇒ where(t.uuid === uuid) select t))
  }

  override def save(user: Identity): Identity = inTransaction {
    val identityId = user.identityId
    val passwordInfo = user.passwordInfo

    val emailRegex = Play.configuration(app).getString("userEmailRegex").getOrElse(".*")
    if (!user.email.exists(_.matches(emailRegex))) {
      throw ForbiddenEx(s"email ${user.email.getOrElse("none")} doesn't match mask $emailRegex")
    }

    val appUser = User(
      id = 0,
      providerId = identityId.providerId,
      providerUserId = identityId.userId,
      email = user.email,
      firstName = user.firstName,
      lastName = user.lastName,
      avatarUrl = user.avatarUrl,
      hasher = passwordInfo.map(_.hasher),
      password = passwordInfo.map(_.password),
      salt = passwordInfo.flatMap(_.salt))

    from(AppDB.userTable)(u ⇒ where(u.providerId === identityId.providerId and u.providerUserId === identityId.userId) select u).singleOption match {
      case None ⇒ AppDB.userTable.insert(appUser)
      case Some(u) ⇒
        AppDB.userTable.update(u.copy(
          email = user.email,
          firstName = user.firstName,
          lastName = user.lastName,
          password = appUser.password,
          salt = appUser.salt,
          hasher = appUser.hasher,
          avatarUrl = appUser.avatarUrl
        ))
        u
    }
  }

  override def save(token: Token): Unit = inTransaction {
    AppDB.tokenTable.insert(token)
  }

  override def deleteExpiredTokens(): Unit = inTransaction {
    AppDB.tokenTable.delete(from(AppDB.tokenTable)(t ⇒ where(t.expirationTime <= DateTime.now()) select t))
  }

  override def findToken(token: String): Option[Token] = inTransaction {
    from(AppDB.tokenTable)(t ⇒ where(t.uuid === token) select t).singleOption
  }
}
