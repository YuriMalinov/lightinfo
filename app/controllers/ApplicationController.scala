package controllers

import models.{AppDB, Info, Project}
import play.api.mvc._
import system.DbDef._

import scala.collection.mutable.ArrayBuffer

case class InfoDisplay(id: Int, name: String, lineCount: Int, code: Option[String], keywords: String, level: Int, childrenCount: Int, trash: Boolean)

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
        ) select info orderBy(info.parentInfoId, info.name))
        .groupBy(_.parentInfoId.getOrElse(0))
    } else {
      Map[Int, Iterable[Info]]()
    }

    def subList(info: Info, level: Int, noRecursion: Set[Int]): List[InfoDisplay] = {
      val children = data.getOrElse(info.id, Nil)
      val subItems = children.flatMap(i ⇒ if (noRecursion.contains(info.id)) Nil else subList(i, level + 1, noRecursion + info.id))

      List(InfoDisplay(info.id, info.name, info.text.count(_ == '\n'), info.code, info.keywords, level, subItems.size, info.trash)) ++ subItems
    }

    val infoDisplays = data.getOrElse(0, Nil).flatMap(i ⇒ subList(i, 0, Set()))
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

