package controllers

import models.{AppDB, UserInProject, UserStatus}
import play.api.data.Forms._
import play.api.mvc.Controller
import system.DbDef._

object UserInProjectController extends Controller {
  def userList(projectId: Int) = CommonAction.requireProjectAdmin(Some(projectId)) { implicit request ⇒
    val data = from(AppDB.userInProjectTable, AppDB.userTable)((userInProject, user) ⇒
      where(userInProject.userId === user.id and userInProject.projectId === projectId)
        select ((userInProject, user))
    ).toSeq
    val project = AppDB.projectTable.lookup(projectId).getOrElse(throw new NotFoundEx(s"Can't find project with id = $projectId"))
    Ok(views.html.userInProject.userList(data, project))
  }

  def deleteUser() = CommonAction.requireAnyUser { implicit request ⇒
    CommonAction.bindPost(tuple("projectId" → number, "userId" → number)) { case (projectId, userId) ⇒
      Access.require(Access.userIsAdminOfProject(projectId)) {
        AppDB.userInProjectTable.delete(AppDB.userInProjectTable.where(u ⇒ u.projectId === projectId and u.userId === userId))
        Redirect(routes.UserInProjectController.userList(projectId))
      }
    }
  }

  def addUser() = CommonAction.requireAnyUser { implicit request ⇒
    CommonAction.bindPost(tuple("projectId" → number, "email" → text)) { case (projectId, email) ⇒
      Access.require(Access.userIsAdminOfProject(projectId)) {
        val user = AppDB.userTable.where(u ⇒ u.email === email).headOption.getOrElse(throw new NotFoundEx(s"Can't find user with email [$email]"))
        AppDB.userInProjectTable.where(u ⇒ u.projectId === projectId and u.userId === user.id).headOption match {
          case Some(uip) ⇒
          case None ⇒ AppDB.userInProjectTable.insert(UserInProject(user.id, projectId, UserStatus.Active))
        }
        Redirect(routes.UserInProjectController.userList(projectId))
      }
    }
  }

  def changeUser() = CommonAction.requireAnyUser { implicit request ⇒
    CommonAction.bindPost(tuple("projectId" -> number, "userId" -> number, "status" -> number)) { case (projectId, userId, status) ⇒
      Access.require(Access.userIsAdminOfProject(projectId)) {
        AppDB.userInProjectTable.where(u ⇒ u.projectId === projectId and u.userId === userId).headOption match {
          case Some(uip) ⇒
            uip.userStatus = UserStatus(status)
            AppDB.userInProjectTable.update(uip)
            Redirect(routes.UserInProjectController.userList(projectId))
          case None ⇒
            NotFound("Can't find user")
        }
      }
    }
  }
}
