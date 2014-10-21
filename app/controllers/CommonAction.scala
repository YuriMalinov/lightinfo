package controllers

import models.{AppDB, Project}
import play.api.mvc._
import system.DbDef

import DbDef._


import scala.concurrent.Future

case class CommonData(projects: Seq[Project], currentProject: Option[Project])

class CommonRequest[A](val commonData: CommonData, request: Request[A]) extends WrappedRequest[A](request)

object CommonAction extends ActionBuilder[CommonRequest] {
  override protected def invokeBlock[A](request: Request[A], block: (CommonRequest[A]) ⇒ Future[SimpleResult]): Future[SimpleResult] = {
    inTransaction {
      val projects = from(AppDB.projectTable)(p ⇒ select(p) orderBy p.name).toList
      val currentProject = request.session.get("projectId").flatMap(id ⇒ projects.find(_.id == id.toInt))

      val data = new CommonData(projects, currentProject)
      val req = new CommonRequest(data, request)

      block(req)
    }
  }
}
