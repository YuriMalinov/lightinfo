package controllers

import models.{AppDB, Project}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Call, Controller}
import system.DbDef._

object ProjectsController extends Controller with CreateUpdateHandler {
  val projectForm = Form(
    mapping(
      "name" → nonEmptyText,
      "code" → nonEmptyText,
      "description" → nonEmptyText
    )(Project.apply)(Project.unapply))

  def projects(page: Int, pageSize: Int) = CommonAction { implicit request ⇒
    val projectPage = selectPage(from(AppDB.projectTable)(p ⇒ select(p)), page, pageSize)
    Ok(views.html.projects.projectsList(projectPage))
  }

  def delete(projectId: Int) = CommonAction {
    AppDB.projectTable.delete(projectId)
    Redirect(routes.ProjectsController.projects()).flashing("Удалено" → "danger")
  }

  override def editImpl(projectId: Option[Int], save: Boolean) = CommonAction { implicit request ⇒
    val project = projectId.map(projectId ⇒ AppDB.projectTable.get(projectId)).getOrElse(Project("", "", ""))

    val form = if (save) projectForm.bindFromRequest() else projectForm.fill(project)

    if (save && !form.hasErrors) {
      val success = form.get
      project.code = success.code
      project.name = success.name
      project.description = success.description

      val result = AppDB.projectTable.insertOrUpdate(project)
      val route: Call = routes.ProjectsController.edit(result.id)
      Redirect(route).flashing("Успешно сохранено" → "success")
    } else {
      Ok(views.html.projects.projectsForm(form, project))
    }
  }
}

