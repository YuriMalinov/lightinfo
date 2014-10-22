package controllers

import models.{AppDB, Info}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{Action, AnyContent, Controller}
import system.DbDef._

case class InfoData(parentInfoId: Int, name: String, keywords: String, text: String)

object InfoController extends Controller {
  val infoForm = Form(
    mapping(
      "parentInfoId" → number,
      "name" → nonEmptyText,
      "keywords" → text,
      "text" → text
    )(InfoData.apply)(InfoData.unapply)
  )

  def create() = editImpl(None, save = false)

  def createSave() = editImpl(None, save = true)

  def edit(id: Int) = editImpl(Some(id), save = false)

  def editSave(id: Int) = editImpl(Some(id), save = true)

  private def editImpl(id: Option[Int], save: Boolean): Action[AnyContent] = CommonAction { implicit request ⇒
    val info = id match {
      case Some(infoId) ⇒ AppDB.infoTable.lookup(infoId).getOrElse(throw NotFoundEx(s"Can't find info with id = $infoId"))
      case None ⇒ Info(
        projectId = request.commonData.currentProject.getOrElse(throw NotFoundEx("Current project is not set. Go to main page.")).id,
        None, "", "", "", 0
      )
    }

    val data = InfoData(info.parentInfoId.getOrElse(0), info.name, info.keywords, info.text)
    val form = if (save) infoForm.bindFromRequest() else infoForm.fill(data)

    if (save && !form.hasErrors) {
      // Copy fields back
      val r = form.get
      info.name = r.name
      info.keywords = r.keywords
      info.parentInfoId = r.parentInfoId match {
        case 0 ⇒ None
        case parentId ⇒ Some(AppDB.infoTable.lookup(parentId).getOrElse(throw NotFoundEx(s"Can't find parentInfo with id = $parentId, are you cheating?")).id)
      }
      info.text = r.text

      val saved = AppDB.infoTable.insertOrUpdate(info)

      Redirect(routes.InfoController.edit(saved.id)).flashing("Успешно сохранено" → "success")
    } else {
      Ok(views.html.info.infoEdit(form, info))
    }
  }
}
