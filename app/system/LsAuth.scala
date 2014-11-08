package system

import play.api.Application
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import play.api.libs.ws.WS
import securesocial.core.{SecureSocial, SocialUser, OAuth1Provider}

class LsAuth(app: Application) extends OAuth1Provider(app) {
  override def fillProfile(user: SocialUser): SocialUser = {
    val oauthInfo = user.oAuth1Info.get
    val call = WS.url(LsAuth.GetUserName).sign(
      OAuthCalculator(SecureSocial.serviceInfoFor(user).get.key,
        RequestToken(oauthInfo.token, oauthInfo.secret))
    ).get()

    user
  }

  override def id: String = "lightsoft"
}

object LsAuth {
  val GetUserName = "http://passport.ls1.ru/auth/api?action=username"
}