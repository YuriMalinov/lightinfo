package controllers

import java.io.InputStreamReader

import models._
import org.jsoup.Jsoup
import org.mozilla.javascript.{Context, JavaScriptException, NativeFunction}
import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.{ContentTypeOf, Writeable}
import play.api.libs.json.{JsNumber, JsObject, JsString}
import play.api.mvc.{Action, AnyContent, Controller}
import system.DbDef._

import scalax.file.Path

case class InfoData(parentInfoId: Int, name: String, keywords: String, text: String, code: Option[String], isPrivate: Boolean)

case class InfoRevisionData(rev: InfoRevision, user: User, parentInfo: Option[Info])

object InfoController extends Controller {
  def create(projectCode: String, code: String = "") = editImpl(projectCode, None, save = false)

  def createSave(projectCode: String) = editImpl(projectCode, None, save = true)

  def createSaveAjax(projectCode: String) = editImpl(projectCode, None, save = true, ajax = true)

  def edit(projectCode: String, id: Int) = editImpl(projectCode, Some(id), save = false)

  def editSave(projectCode: String, id: Int) = editImpl(projectCode, Some(id), save = true)

  def editSaveAjax(projectCode: String, id: Int) = editImpl(projectCode, Some(id), save = true, ajax = true)

  private def editImpl(projectCode: String, id: Option[Int], save: Boolean, code: String = "", ajax: Boolean = false): Action[AnyContent] = CommonAction.requireAnyUser { implicit request ⇒
    val info = id match {
      case Some(infoId) ⇒ AppDB.infoTable.lookup404(infoId)
      case None ⇒ Info(
        projectId = Project.findByCode(projectCode).id,
        None, "", "", if (code.isEmpty) None else Some(code), "", 0,
        isPrivate = false,
        trash = false
      )
    }

    val infoForm = Form(
      mapping(
        "parentInfoId" → number,
        "name" → nonEmptyText,
        "keywords" → text,
        "text" → text,
        "code" → optional(text verifying("Код должен быть уникальным", code ⇒
          AppDB.infoTable.where(i ⇒ i.projectId === info.projectId and i.code === code and i.id <> id.?).isEmpty)),
        "isPrivate" → boolean
      )(InfoData.apply)(InfoData.unapply)
    )


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

        if (ajax) {
          Ok(JsObject(Seq(
            "result" → JsString("OK"),
            "id" → JsNumber(saved.id)
          )))
        } else {
          Redirect(routes.InfoController.edit(projectCode, saved.id)).flashing("Успешно сохранено" → "success")
        }
      } else {
        if (ajax) {
          Ok(JsObject(Seq(
            "result" → JsString("ERROR"),
            "errors" → form.errorsAsJson
          )))
        } else {
          val infos = Info.sortByParent(Info.findByProject(info.projectId).filter(i ⇒ i.id != info.id && !i.trash))
          val options = infos.map(i ⇒ i.id.toString → ((if (i.level > 0) "-" * i.level + " " else "") + i.name))
          Ok(views.html.info.infoEdit(form, info, ("0" → "Нет") +: options))
        }
      }
    }
  }

  def trash(projectCode: String, infoId: Int, trash: Boolean) = CommonAction.requireAnyUser { implicit request =>
    Access.require(Access.getInfoAccess(infoId).edit) {
      inTransaction {
        val info = AppDB.infoTable.lookup404(infoId)
        info.trash = trash
        AppDB.infoTable.update(info)
        Redirect(routes.ApplicationController.index(info.project.head.code))
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
      AppDB.infoImageTable.where(i ⇒ i.infoId === infoId and i.id === imageId).singleOption match {
        case Some(image) ⇒ Ok(image)(Writeable((i: InfoImage) ⇒ i.data)(ContentTypeOf(Some(image.contentType))))
        case None ⇒ BadRequest(s"Image with id $imageId for info $infoId not found.")
      }
    }
  }

  def viewInfo(infoId: Int) = CommonAction { implicit request ⇒
    val info = AppDB.infoTable.lookup(infoId).getOrElse(throw NotFoundEx(s"Can't find info $infoId"))
    viewInfoImpl(info)
  }

  def viewInfoImpl(info: Info)(implicit request: CommonRequest[AnyContent]) = {
    val access = Access.getInfoAccess(request.user, info.projectId)
    Access.require(access.view) {
      val cx = Context.enter()
      try {
        implicit val app = Play.current
        val scope = cx.initStandardObjects()

        def importJs(name: String): Unit = {
          val reader = new InputStreamReader(Play.resourceAsStream(name).get, "utf-8")
          cx.evaluateReader(scope, reader, name, 1, null)
        }

        importJs("public/js/marked.js")
        importJs("public/highlightjs/highlight.pack.js")
        importJs("public/javascript/info-render.js")

        val renderInfo = cx.evaluateString(scope, "renderInfo", "", 1, null).asInstanceOf[NativeFunction]
        val renderResult = try {
          renderInfo.call(cx, scope, scope, Array(info.text, false: java.lang.Boolean)).asInstanceOf[String]
        } catch {
          case e: JavaScriptException =>
            val x = e // debug
            println(e.getScriptStackTrace)
            throw e
        }

        val htmlResult = if (access.viewInternal) {
          renderResult
        } else {
          val doc = Jsoup.parse(renderResult)
          doc.select(".dev-section").remove()
          doc.toString
        }

        Ok(views.html.info.infoView(info, info.project.single, access, htmlResult))
      } finally {
        Context.exit()
      }
    }
  }

  def viewInfoByCode(code: String, projectCode: String) = CommonAction { implicit request ⇒
    val project = findProjectByCode(projectCode)
    val access = Access.getInfoAccess(request.user, project.id)
    Access.require(access.view) {
      AppDB.infoTable.where(i ⇒ i.code === code and i.projectId === project.id).singleOption match {
        case Some(info) ⇒ viewInfoImpl(info)
        case None ⇒ Ok(views.html.info.infoNotFound(code))
      }
    }
  }

  def findProjectByCode(projectCode: String): Project = {
    AppDB.projectTable.where(p ⇒ p.code === projectCode).singleOption.getOrElse(throw NotFoundEx(s"Can't find project with code $projectCode"))
  }

  def checkInfoByCode(code: String, projectCode: String, callback: String) = CommonAction.withUser { implicit request ⇒
    val project = findProjectByCode(projectCode)
    inTransaction {
      val access = Access.getInfoAccess(request.user, project.id)

      val result = callback + "(\"" + JsObject(Seq(
        "result" → JsString(
          AppDB.infoTable.where(i ⇒ i.code === code and i.projectId === project.id).singleOption match {
            case Some(info) ⇒ "exists"
            case None ⇒ if (access.edit) "can-create" else "none"
          }),
        "route" → JsString(routes.InfoController.create(code).absoluteURL())
      )) + "\")"
      Ok(result)
    }
  }

  def revisions(infoId: Int, page: Int, pageSize: Int) = CommonAction.requireAnyUser { implicit request ⇒
    val info = AppDB.infoTable.lookup404(infoId)
    Access.require(Access.getInfoAccess(info.projectId).edit) {
      val revisionPage = selectPage(from(AppDB.infoRevisionTable, AppDB.userTable)((r, u) ⇒
        where(u.id === r.userId and r.infoId === infoId)
          select ((u, r))
      ), page, pageSize)

      Ok(views.html.info.infoRevisions(revisionPage, info))
    }
  }

  def viewRevision(revisionId: Int) = CommonAction.requireAnyUser { implicit request ⇒
    val revision = AppDB.infoRevisionTable.lookup404(revisionId)
    Access.require(Access.getInfoAccess(revision.info.single.projectId).edit, "No access to info") {
      val previous = from(AppDB.infoRevisionTable)(r ⇒ where(r.infoId === revision.infoId and r.revisionDate < revision.revisionDate) select r orderBy r.id.desc).page(0, 1).singleOption

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

        Redirect(routes.InfoController.edit(info.project.single.code, revision.infoId))
      }
    }
  }
}
