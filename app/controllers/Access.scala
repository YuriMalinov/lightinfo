package controllers

import models.{AppDB, ProjectType, User, UserStatus}
import system.DbDef._

case class InfoAccess(view: Boolean, viewInternal: Boolean, edit: Boolean, isMemberOfProject: Boolean, isLoggedIn: Boolean)

object Access {
  def userIsAdminOfProject(user: User, projectId: Option[Int]): Option[String] = projectId.flatMap(p ⇒ userIsAdminOfProject(user.id, p))

  def userIsAdminOfProject(projectId: Int)(implicit request: CommonRequest[_]): Option[String] = userIsAdminOfProject(request.user.get.id, projectId)

  def userIsAdminOfProject(userId: Int, projectId: Int): Option[String] = {
    AppDB.userInProjectTable.where(u ⇒
      u.projectId === projectId
        and u.userId === userId
        and u.userStatus === UserStatus.Admin
    ).singleOption match {
      case Some(_) ⇒ None
      case None ⇒
        // If user doesn't have admin rights, check if anybody does. If all admins are gone then anyone could edit project.
        AppDB.userInProjectTable.where(u ⇒
          u.projectId === projectId
            and u.userStatus === UserStatus.Admin
        ).singleOption match {
          // Some admin is found.
          case Some(_) ⇒ Some(s"Нет доступа к проекту [$projectId]")
          case None ⇒ None
        }
    }
  }

  @inline def require[T](check: Option[String])(block: ⇒ T): T = {
    check match {
      case Some(error) ⇒ throw new ForbiddenEx(error)
      case None ⇒ block
    }
  }

  @inline def require[T](hasAccess: Boolean, description: ⇒ String = "No access")(block: ⇒ T): T = {
    if (hasAccess) {
      block
    } else {
      throw new ForbiddenEx(description)
    }
  }

  def getInfoAccess(infoId: Int)(implicit request: CommonRequest[_]): InfoAccess = {
    val info = AppDB.infoTable.lookup(infoId).getOrElse(throw NotFoundEx(s"Can't find info with id = $infoId"))
    getInfoAccess(request.user, infoId)
  }

  def getInfoAccess(user: Option[User], projectId: Int): InfoAccess = inTransaction {
    val project = AppDB.projectTable.get(projectId)
    val userInProject = user.flatMap(u ⇒ AppDB.userInProjectTable.where(uu ⇒ uu.userId === u.id and uu.projectId === projectId).singleOption)

    val userHasAccessToProject = userInProject.exists(u ⇒ u.userStatus == UserStatus.Active || u.userStatus == UserStatus.Admin)
    val canView = project.projectType match {
      case ProjectType.Public ⇒ true
      case ProjectType.Protected ⇒ user.isDefined
      case ProjectType.Private ⇒ userHasAccessToProject
    }

    InfoAccess(
      view = canView,
      viewInternal = canView && user.isDefined,
      edit = userHasAccessToProject,
      isLoggedIn = user.isDefined,
      isMemberOfProject = userHasAccessToProject
    )
  }
}
