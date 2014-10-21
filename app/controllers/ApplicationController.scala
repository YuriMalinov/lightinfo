package controllers

import models.AppDB
import play.api.mvc._
import system.DbDef._

import scala.collection.mutable.ArrayBuffer

object ApplicationController extends Controller {
  def index = CommonAction { implicit request ⇒
    Ok(views.html.index())
  }

  def selectProject(projectId: Int) = CommonAction { request ⇒
    from(AppDB.projectTable)(p ⇒ where(p.id === projectId) select p).headOption match {
      case Some(project) ⇒ Redirect(routes.ApplicationController.index())
        .withSession(request.session + ("projectId" → project.id.toString))
      case None ⇒ BadRequest(s"Can't find project with id = $projectId.")
    }
  }

  def ddl = CommonAction { implicit request ⇒
    val sql = new ArrayBuffer[String]()
    AppDB.printDdl(sql += _)
    Ok(views.html.ddl(sql.mkString("\n")))
  }
}

