package controllers

import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.{ContentTypeOf, Writeable}
import play.api.mvc.{Action, AnyContent, Controller}
import system.DbDef._

import scalax.file.Path

case class InfoData(parentInfoId: Int, name: String, keywords: String, text: String, code: String, isPrivate: Boolean)

case class InfoRevisionData(rev: InfoRevision, user: User, parentInfo: Option[Info])

object InfoController extends Controller {
  val infoForm = Form(
    mapping(
      "parentInfoId" → number,
      "name" → nonEmptyText,
      "keywords" → text,
      "text" → text,
      "code" → text,
      "isPrivate" → boolean
    )(InfoData.apply)(InfoData.unapply)
  )

  def create() = editImpl(None, save = false)

  def createSave() = editImpl(None, save = true)

  def edit(id: Int) = editImpl(Some(id), save = false)

  def editSave(id: Int) = editImpl(Some(id), save = true)

  private def editImpl(id: Option[Int], save: Boolean): Action[AnyContent] = CommonAction.requireAnyUser { implicit request ⇒
    val info = id match {
      case Some(infoId) ⇒ AppDB.infoTable.lookup(infoId).getOrElse(throw NotFoundEx(s"Can't find info with id = $infoId"))
      case None ⇒ Info(
        projectId = request.commonData.currentProject.getOrElse(throw NotFoundEx("Current project is not set. Go to main page.")).id,
        None, "", "", "", "", 0, isPrivate = false
      )
    }

    Access.require(Access.getInfoAccess(request.user, info.projectId).edit, "No access to info create/edit") {
      val data = InfoData(info.parentInfoId.getOrElse(0), info.name, info.keywords, info.text, info.code, info.isPrivate)
      val form = if (save) infoForm.bindFromRequest() else infoForm.fill(data)

      if (save && !form.hasErrors) {
        // Copy fields back
        val r = form.get
        info.name = r.name
        info.keywords = r.keywords
        info.code = r.code
        info.isPrivate = r.isPrivate
        info.parentInfoId = r.parentInfoId match {
          case 0 ⇒ None
          case parentId ⇒ Some(AppDB.infoTable.lookup(parentId).getOrElse(throw NotFoundEx(s"Can't find parentInfo with id = $parentId, are you cheating?")).id)
        }
        info.text = r.text

        val saved = AppDB.infoTable.insertOrUpdate(info)

        AppDB.infoRevisionTable.insert(saved.makeRevision(request.user.get.id))

        Redirect(routes.InfoController.edit(saved.id)).flashing("Успешно сохранено" → "success")
      } else {
        Ok(views.html.info.infoEdit(form, info))
      }
    }
  }

  def uploadImage(infoId: Int) = CommonAction.requireAnyUser { implicit request ⇒
    Access.require(Access.getInfoAccess(infoId).edit) {
      val data = request.body.asMultipartFormData.getOrElse(throw BadRequestEx("Request should be multipart/form-data"))

      val file = data.file("data").getOrElse(throw BadRequestEx("data field not found"))

      // TODO: Sometime in future it'll be good to check if image is image and guess it's content-type through some clever lib
      val image = new InfoImage(infoId, Path(file.ref.file).byteArray, file.contentType.getOrElse("image/png"))

      val id = inTransaction {
        AppDB.infoImageTable.insert(image).id
      }

      Ok(routes.InfoController.getImage(image.infoId, image.id).url)
    }
  }

  def getImage(infoId: Int, imageId: Long) = Action {
    inTransaction {
      from(AppDB.infoImageTable) { i ⇒ where(i.infoId === infoId and i.id === imageId) select i}.headOption match {
        case Some(image) ⇒ Ok(image)(Writeable((i: InfoImage) ⇒ i.data)(ContentTypeOf(Some(image.contentType))))
        case None ⇒ BadRequest(s"Image with id $imageId for info $infoId not found.")
      }
    }
  }

  def viewInfo(infoId: Int) = CommonAction { implicit request ⇒
    val info = AppDB.infoTable.lookup(infoId).getOrElse(throw NotFoundEx(s"Can't find info $infoId"))
    val access = Access.getInfoAccess(request.user, info.projectId)
    Access.require(access.view) {
      // TODO: implement server side rendering when Internet would be available and Rhino could be downloaded.
      Ok(views.html.info.infoView(info, access))
    }
  }

  def revisions(infoId: Int, page: Int, pageSize: Int) = CommonAction.requireAnyUser { implicit request ⇒
    val info = AppDB.infoTable.lookup404(infoId)
    Access.require(Access.getInfoAccess(infoId).edit) {
      val revisionPage = selectPage(from(AppDB.infoRevisionTable, AppDB.userTable)((r, u) ⇒
        where(u.id === r.userId and r.infoId === infoId)
          select ((u, r))
      ), page, pageSize)

      Ok(views.html.info.infoRevisions(revisionPage, info))
    }
  }

  def viewRevision(revisionId: Int) = CommonAction.requireAnyUser { implicit request ⇒
    val revision = AppDB.infoRevisionTable.lookup404(revisionId)
    Access.require(Access.getInfoAccess(revision.infoId).edit, "No access to info") {
      val previous = from(AppDB.infoRevisionTable)(r ⇒ where(r.infoId === revision.infoId and r.revisionDate < revision.revisionDate) select r orderBy r.id.desc).headOption

      def revData(rev: InfoRevision) = InfoRevisionData(
        rev = rev,
        user = rev.user.single,
        parentInfo = rev.parentInfoId.map(id ⇒ AppDB.infoTable.lookup404(id))
      )

      Ok(views.html.info.infoRevision(revData(revision), previous.map(revData)))
    }
  }

  def restoreRevision() = CommonAction.requireAnyUser { implicit request ⇒
    CommonAction.bindPost(single("revisionId" → number)) { revisionId ⇒
      val revision = AppDB.infoRevisionTable.lookup404(revisionId)
      Access.require(Access.getInfoAccess(revision.infoId).edit) {
        val info = revision.info.single
        info.restoreRevision(revision)
        AppDB.infoTable.update(info)
        AppDB.infoRevisionTable.insert(info.makeRevision(request.user.get.id))

        Redirect(routes.InfoController.edit(revision.infoId))
      }
    }
  }
}
