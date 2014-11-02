package controllers

import models.{UserStatus, AppDB, User}
import system.DbDef._

object Access {
  def userIsAdminOfProject(user: User, projectId: Option[Int]): Option[String] = projectId.flatMap(p ⇒ userIsAdminOfProject(user.id, p))

  def userIsAdminOfProject(projectId: Int)(implicit request: CommonRequest[_]): Option[String] = userIsAdminOfProject(request.user.get.id, projectId)

  def userIsAdminOfProject(userId: Int, projectId: Int): Option[String] = {
    AppDB.userInProjectTable.where(u ⇒
      u.projectId === projectId
        and u.userId === userId
        and u.userStatus === UserStatus.Admin
    ).headOption match {
      case Some(_) ⇒ None
      case None ⇒
        // If user doesn't have admin rights, check if anybody does. If all admins are gone then anyone could edit project.
        AppDB.userInProjectTable.where(u ⇒
          u.projectId === projectId
            and u.userStatus === UserStatus.Admin
        ).headOption match {
          // Some admin is found.
          case Some(_) ⇒ Some(s"Нет доступа к проекту [$projectId]")
          case None ⇒ None
        }
    }
  }

  def require[T](check: Option[String])(block: ⇒ T): T = {
    check match {
      case Some(error) ⇒ throw new ForbiddenEx(error)
      case None ⇒ block
    }
  }
}
