package system

import controllers.CommonRequest
import play.api.Application
import play.api.data.Form
import play.api.mvc.{Request, RequestHeader}
import play.api.templates.{Html, Txt}
import securesocial.controllers.PasswordChange.ChangeInfo
import securesocial.controllers.Registration.RegistrationInfo
import securesocial.controllers.TemplatesPlugin
import securesocial.core.{Identity, SecuredRequest}

class SecureSocialTemplatesPlugin(app: Application) extends TemplatesPlugin {
  override def getLoginPage[A](implicit request: Request[A], form: Form[(String, String)],
                               msg: Option[String] = None): Html = {
    views.html.ss.login(form, msg)(request)
  }

  override def getSignUpPage[A](implicit request: Request[A], form: Form[RegistrationInfo], token: String): Html = {
    views.html.ss.Registration.signUp(form, token)(request)
  }

  override def getStartSignUpPage[A](implicit request: Request[A], form: Form[String]): Html = {
    views.html.ss.Registration.startSignUp(form)(request)
  }

  override def getStartResetPasswordPage[A](implicit request: Request[A], form: Form[String]): Html = {
    views.html.ss.Registration.startResetPassword(form)(request)
  }

  def getResetPasswordPage[A](implicit request: Request[A], form: Form[(String, String)], token: String): Html = {
    views.html.ss.Registration.resetPasswordPage(form, token)(request)
  }

  def getPasswordChangePage[A](implicit request: SecuredRequest[A], form: Form[ChangeInfo]): Html = {
    views.html.ss.passwordChange(form)(request)
  }

  def getNotAuthorizedPage[A](implicit request: Request[A]): Html = {
    views.html.ss.notAuthorized()(request)
  }

  def getSignUpEmail(token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.ss.mails.signUpEmail(token)))
  }

  def getAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.ss.mails.alreadyRegisteredEmail(user)))
  }

  def getWelcomeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.ss.mails.welcomeEmail(user)))
  }

  def getUnknownEmailNotice()(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.ss.mails.unknownEmailNotice(request)))
  }

  def getSendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.ss.mails.passwordResetEmail(user, token)))
  }

  def getPasswordChangedNoticeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    (None, Some(views.html.ss.mails.passwordChangedNotice(user)))
  }
}
