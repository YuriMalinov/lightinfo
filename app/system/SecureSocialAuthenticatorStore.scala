package system

import models.{AuthenticatorHolder, AppDB}
import play.api.Application
import play.api.cache.Cache
import securesocial.core.{Authenticator, AuthenticatorStore}
import system.DbDef._

class SecureSocialAuthenticatorStore(implicit app: Application) extends AuthenticatorStore(app) {
  override def save(authenticator: Authenticator): Either[Error, Unit] = inTransaction {
    saveInCache(authenticator)
    AppDB.authenticatorTable.insert(new AuthenticatorHolder(authenticator))
    Right()
  }

  private def saveInCache(authenticator: Authenticator) = {
    Cache.set(authenticator.id, authenticator, Authenticator.absoluteTimeoutInSeconds)
    authenticator
  }

  override def delete(id: String): Either[Error, Unit] = inTransaction {
    Cache.remove(id)
    AppDB.authenticatorTable.delete(id)
    Right()
  }

  override def find(id: String): Either[Error, Option[Authenticator]] = {
    Right(Cache.getAs[Authenticator](id).fold {
      inTransaction {
        val a = from(AppDB.authenticatorTable)(a ⇒ where(a.id === id) select a).headOption.map { a ⇒
          saveInCache(a.toAuthenticator)
        }
        a
      }
    }(Some(_)))
  }
}
