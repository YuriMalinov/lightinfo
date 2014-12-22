package controllers

import models._
import play.api.data.{Form, Mapping}
import play.api.mvc._
import securesocial.core.{SecureSocial, UserService}
import system.DbDef._

import scala.concurrent.Future

case class CommonData(projects: Seq[Project], currentProject: Option[Project])

class RequestWithUser[A](val user: Option[User], request: Request[A]) extends WrappedRequest[A](request)

class CommonRequest[A](val commonData: CommonData, user: Option[User], request: Request[A]) extends RequestWithUser[A](user, request) {
  implicit val cache = new Cache
}

object CommonRequest {
  implicit def requestToCommonRequest[A](request: Request[A]): CommonRequest[A] = {
    inTransaction {
      val user = SecureSocial.authenticatorFromRequest(request)
        .flatMap(auth ⇒ UserService.find(auth.identityId)).asInstanceOf[Option[User]]

      val projects = from(AppDB.projectTable)(p ⇒
        where(
          p.allowRequestForAccess === true
            or p.projectType === ProjectType.Public
            // TODO: Разобраться с inhibitWhen + exists
            or exists(from(AppDB.userInProjectTable)(u ⇒ where(u.userId === user.map(_.id).getOrElse(0) and u.userStatus.in(Seq(UserStatus.Active, UserStatus.Admin))) select u))
        ) select p orderBy p.name).toList
      val currentProject = request.session.get("projectId").flatMap(id ⇒ projects.find(_.id == id.toInt))


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

  def apply(checkUser: Option[User] ⇒ Option[String], block: CommonRequest[AnyContent] ⇒ Result): Action[AnyContent] = apply { request ⇒
    checkUser(request.user) match {
      case Some(error) ⇒ ApplicationController.Forbidden(error)
      case None ⇒ block(request)
    }
  }

  def requireUser(checkUser: User ⇒ Option[String])(block: CommonRequest[AnyContent] ⇒ Result): Action[AnyContent] = apply { request ⇒
    inTransaction {
      if (request.user.isEmpty) {
        ApplicationController.Forbidden("Authorization required")
      } else {
        checkUser(request.user.get) match {
          case Some(error) ⇒ ApplicationController.Forbidden(error)
          case None ⇒ block(request)
        }
      }
    }
  }

  def requireAnyUser(block: CommonRequest[AnyContent] ⇒ Result): Action[AnyContent] = apply { request ⇒
    inTransaction {
      if (request.user.isEmpty) {
        ApplicationController.Forbidden("Authorization required")
      } else {
        block(request)
      }
    }
  }

  def withUser(block: RequestWithUser[AnyContent] ⇒ Result): Action[AnyContent] = Action { request ⇒
    inTransaction {
      val user = SecureSocial.authenticatorFromRequest(request)
        .flatMap(auth ⇒ UserService.find(auth.identityId)).asInstanceOf[Option[User]]

      block(new RequestWithUser[AnyContent](user, request))
    }
  }

  def requireProjectAdmin(projectId: Option[Int])(block: CommonRequest[AnyContent] ⇒ Result) = requireUser(u ⇒ Access.userIsAdminOfProject(u, projectId))(block)

  def bindPost[T, A <: SimpleResult](binding: Mapping[T])(block: T ⇒ A)(implicit request: Request[AnyContent]): SimpleResult = {
    val result = Form(binding).bindFromRequest()
    result.value match {
      case Some(v) ⇒ block(v)
      case None ⇒ ApplicationController.BadRequest(result.errorsAsJson.toString())
    }
  }
}
