package controllers

import models.{AppDB, Info, Project}
import play.api.mvc._
import system.DbDef._

import scala.collection.mutable.ArrayBuffer


object ApplicationController extends Controller {
  def simpleIndex = CommonAction { implicit request ⇒
    Ok(views.html.indexWithoutProject())
  }

  def index(projectCode: String) = CommonAction { implicit request ⇒
    val project = Project.findByCode(projectCode)
    val access = Access.getInfoAccess(request.user, project.id)
    val trash = request.getQueryString("trash").exists(_ == "1")

    val data = if (access.view) {
      from(AppDB.infoTable)(info ⇒
        where(info.projectId === project.id
          and (info.isPrivate === false).inhibitWhen(access.viewInternal)
          and (info.trash === false).inhibitWhen(access.viewInternal && trash)
        ) select info orderBy(info.parentInfoId, info.name)).toSeq
    } else {
      Seq[Info]()
    }

    val infoDisplays = Info.sortByParent(data)

    Ok(views.html.index(project, infoDisplays, access, trash))
  }

  def selectProject(projectId: Int) = CommonAction { implicit request ⇒
    AppDB.projectTable.lookup(projectId) match {
      case Some(project) ⇒ Redirect(routes.ApplicationController.index(project.code)).withSession(session + ("projectId" → projectId.toString))
      case None ⇒ BadRequest(s"Can't find project with id = $projectId.")
    }
  }

  def ddl = Action { implicit request ⇒
    inTransaction {
      val sql = new ArrayBuffer[String]()
      AppDB.printDdl(sql += _)
      Ok(views.html.ddl(sql.mkString("\n")))
    }
  }
}

