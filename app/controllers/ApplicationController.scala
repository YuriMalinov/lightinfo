package controllers

import java.text.DateFormat
import java.util.{Locale, TimeZone, Calendar}

import models.{Project, AppDB, Info}
import play.api.mvc._
import system.DbDef._

import scala.collection.mutable.ArrayBuffer

case class InfoDisplay(id: Int, name: String, code: Option[String], keywords: String, level: Int, childrenCount: Int)

object ApplicationController extends Controller {
  def simpleIndex = CommonAction { implicit request ⇒
    Ok(views.html.indexWithoutProject())
  }

  def index(projectCode: String) = CommonAction { implicit request ⇒
    val project = Project.findByCode(projectCode)
    val access = Access.getInfoAccess(request.user, project.id)

    val data = if (access.view) {
      from(AppDB.infoTable)(info ⇒ where(info.projectId === project.id and (info.isPrivate === false).inhibitWhen(access.viewInternal)) select info orderBy(info.parentInfoId, info.name))
        .groupBy(_.parentInfoId.getOrElse(0))
    } else {
      Map[Int, Iterable[Info]]()
    }

    def subList(info: Info, level: Int, noRecursion: Set[Int]): List[InfoDisplay] = {
      List(InfoDisplay(info.id, info.name, info.code, info.keywords, level, info.childrenCount)) ++ data.getOrElse(info.id, Nil)
        .flatMap(i ⇒ if (noRecursion.contains(info.id)) Nil else subList(i, level + 1, noRecursion + info.id))
    }

    val infoDisplays = data.getOrElse(0, Nil).flatMap(i ⇒ subList(i, 0, Set()))
    Ok(views.html.index(project, infoDisplays, access))
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

