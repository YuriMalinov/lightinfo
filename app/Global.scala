import controllers.{ForbiddenEx, BadRequestEx, ApplicationController, NotFoundEx}
import org.squeryl.adapters.H2Adapter
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.logging.{StatementInvocationEvent, StatisticsListener}
import org.squeryl.{Session, SessionFactory}
import play.api.mvc.{SimpleResult, RequestHeader}
import play.api.{Application, GlobalSettings}
import play.api.db.DB
import play.mvc.Http.Status
import system.MyPostgreSqlAdapter

import scala.concurrent.Future

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    SessionFactory.concreteFactory = app.configuration.getString("db.default.driver") match {
      case Some("org.h2.Driver") => Some(() => getSession(new H2Adapter, app))
      case Some("org.postgresql.Driver") => Some(() => getSession(new MyPostgreSqlAdapter, app))
      case _ => sys.error("Database driver must be either org.h2.Driver or org.postgresql.Driver")
    }
  }

  def getSession(adapter: DatabaseAdapter, app: Application) = {
    val session = Session.create(DB.getConnection()(app), adapter)
    if (app.configuration.getBoolean("squeryl.trace").getOrElse(false)) {
      session.setLogger(println)
    }
    session
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] = {
    ex match {
      case NotFoundEx(message) ⇒ Future.successful(ApplicationController.NotFound(message))
      case BadRequestEx(message) ⇒ Future.successful(ApplicationController.BadRequest(message))
      case ForbiddenEx(message) ⇒ Future.successful(ApplicationController.Forbidden(message))
      case e: Throwable ⇒ super.onError(request, ex)
    }
  }
}