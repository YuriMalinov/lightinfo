package controllers

import java.text.DateFormat
import java.util.{Locale, TimeZone, Calendar}

import models.{AppDB, Info}
import play.api.mvc._
import system.DbDef._

import scala.collection.mutable.ArrayBuffer

case class InfoDisplay(id: Int, name: String, code: String, keywords: String, level: Int, childrenCount: Int)

object ApplicationController extends Controller {
  def index = CommonAction { implicit request ⇒
    request.commonData.currentProject match {
      case None ⇒
        Ok(views.html.indexWithoutProject())
      case Some(project) ⇒
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
        Ok(views.html.index(infoDisplays, access))
    }
  }

  def selectProject(projectId: Int) = CommonAction { implicit request ⇒
    AppDB.projectTable.lookup(projectId) match {
      case Some(project) ⇒ selectProjectModifySession(Redirect(routes.ApplicationController.index()), project.id)
      case None ⇒ BadRequest(s"Can't find project with id = $projectId.")
    }
  }

  def selectProjectModifySession(result: SimpleResult, projectId: Int)(implicit request: Request[_]): SimpleResult = {
    result.withSession(request.session + ("projectId" → projectId.toString))
  }

  def ddl = Action { implicit request ⇒
    inTransaction {
      val sql = new ArrayBuffer[String]()
      AppDB.printDdl(sql += _)
      Ok(views.html.ddl(sql.mkString("\n")))
    }
  }
}

