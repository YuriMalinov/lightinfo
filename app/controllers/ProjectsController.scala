package controllers

import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Controller
import system.DbDef._

object ProjectsController extends Controller {
  val projectForm = Form(
    mapping(
      "name" → nonEmptyText,
      "code" → nonEmptyText,
      "description" → nonEmptyText,
      "projectType" → ProjectTypeMapping.mapping,
      "allowRequestForAccess" → boolean
    )(Project.apply)(Project.unapply))

  def projects(page: Int, pageSize: Int) = CommonAction.requireAnyUser { implicit request ⇒
    val projectPage = selectPage(
      from(AppDB.projectTable)(p ⇒
        where(
          exists(
            from(AppDB.userInProjectTable)(u ⇒
              where(u.projectId === p.id and u.userId === request.user.get.id and u.userStatus === UserStatus.Admin)
                select u
            ))
            or notExists(
            from(AppDB.userInProjectTable)(u ⇒
              where(u.projectId === p.id and u.userStatus === UserStatus.Admin)
                select u
            ))
        ) select p), page, pageSize)
    val query = from(AppDB.userInProjectTable)(u ⇒ where(u.projectId in projectPage.list.map(_.id)) groupBy(u.projectId, u.userStatus) compute count)
    // Cast is required... I think that PrimitiveTypeMode converts enum to just Enumeration#Value so type is lost
    val userData = query.groupBy(_.key._1).map { case (k, v) ⇒ k → v.map(r ⇒ r.key._2.asInstanceOf[UserStatus.Value] → r.measures).toSeq.sortBy(_._1.id)}
    Ok(views.html.projects.projectsList(projectPage, userData))
  }

  def delete(projectId: Int) = CommonAction.requireUser(u ⇒ Access.userIsAdminOfProject(u, Some(projectId))) { request ⇒
    AppDB.projectTable.delete(projectId)
    Redirect(routes.ProjectsController.projects()).flashing("Удалено" → "danger")
  }

  def create() = editImpl(None, save = false)

  def createSave() = editImpl(None, save = true)

  def edit(id: Int) = editImpl(Some(id), save = false)

  def editSave(id: Int) = editImpl(Some(id), save = true)


  def editImpl(projectId: Option[Int], save: Boolean) = CommonAction.requireUser(u ⇒ Access.userIsAdminOfProject(u, projectId)) {
    implicit request ⇒
      val project = projectId.map(projectId ⇒ AppDB.projectTable.get(projectId)).getOrElse(new Project())

      val form = if (save) projectForm.bindFromRequest() else projectForm.fill(project)

      if (save && !form.hasErrors) {
        val success = form.get
        project.code = success.code
        project.name = success.name
        project.description = success.description
        project.projectType = success.projectType
        project.allowRequestForAccess = success.allowRequestForAccess

        val result = AppDB.projectTable.insertOrUpdate(project)
        if (projectId.isEmpty) {
          AppDB.userInProjectTable.insert(UserInProject(request.user.get.id, result.id, UserStatus.Admin))
        }

        Redirect(routes.ProjectsController.edit(result.id)).flashing("Успешно сохранено" → "success")
      } else {
        Ok(views.html.projects.projectsForm(form, project))
      }
  }
}

