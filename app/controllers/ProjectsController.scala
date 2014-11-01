package controllers

import models.{ProjectTypeMapping, AppDB, Project}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Call, Controller}
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

  def projects(page: Int, pageSize: Int) = CommonAction { implicit request ⇒
    val projectPage = selectPage(from(AppDB.projectTable)(p ⇒ select(p)), page, pageSize)
    Ok(views.html.projects.projectsList(projectPage))
  }

  def delete(projectId: Int) = CommonAction {
    AppDB.projectTable.delete(projectId)
    Redirect(routes.ProjectsController.projects()).flashing("Удалено" → "danger")
  }

  def create() = editImpl(None, save = false)
  def createSave() = editImpl(None, save = true)
  def edit(id: Int) = editImpl(Some(id), save = false)
  def editSave(id: Int) = editImpl(Some(id), save = true)

  def editImpl(projectId: Option[Int], save: Boolean) = CommonAction { implicit request ⇒
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

      Redirect(routes.ProjectsController.edit(result.id)).flashing("Успешно сохранено" → "success")
    } else {
      Ok(views.html.projects.projectsForm(form, project))
    }
  }
}

