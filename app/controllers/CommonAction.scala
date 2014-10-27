package controllers

import models.{User, AppDB, Project}
import play.api.mvc._
import securesocial.core.{UserService, SecureSocial}
import system.DbDef

import DbDef._


import scala.concurrent.Future

case class CommonData(projects: Seq[Project], currentProject: Option[Project])

class CommonRequest[A](val commonData: CommonData, val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

object CommonRequest {
  implicit def requestToCommonRequest[A](request: Request[A]): CommonRequest[A] = {
    inTransaction {
      val projects = from(AppDB.projectTable)(p ⇒ select(p) orderBy p.name).toList
      val currentProject = request.session.get("projectId").flatMap(id ⇒ projects.find(_.id == id.toInt))

      val user = SecureSocial.authenticatorFromRequest(request)
        .flatMap(auth ⇒ UserService.find(auth.identityId)).asInstanceOf[Option[User]]

      val data = new CommonData(projects, currentProject)

      new CommonRequest(data, user, request)
    }
  }
}

object CommonAction extends ActionBuilder[CommonRequest] {
  override protected def invokeBlock[A](request: Request[A], block: (CommonRequest[A]) ⇒ Future[SimpleResult]): Future[SimpleResult] = {
    inTransaction {
      block(request)
    }
  }
}
