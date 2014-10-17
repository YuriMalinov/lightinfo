package controllers

import models.AppDB
import play.api.mvc._
import system.DbDef

import scala.collection.mutable.ArrayBuffer

import DbDef._

object ApplicationController extends Controller {

//  val barForm = Form(
//    mapping(
//      "name" -> optional(text)
//    )(Bar.apply)(Bar.unapply)
//  )

  def index = Action {
    Ok(views.html.index())
  }

  def ddl = Action {
    inTransaction {
      val sql = new ArrayBuffer[String]()
      AppDB.printDdl(sql += _)
      Ok(views.html.ddl(sql.mkString("\n")))
    }
  }


  //  def getBars = Action {
//    implicit val barWrites = Json.writes[Bar]
//    val json = inTransaction {
//      val bars = from(AppDB.barTable)(barTable =>
//        select(barTable)
//      )
//      Json.toJson(bars)
//    }
//    Ok(json)
//  }
//
//  def addBar = Action { implicit request =>
//    barForm.bindFromRequest.value map { bar =>
//      inTransaction(AppDB.barTable insert bar)
//      Redirect(routes.Application.index())
//    } getOrElse BadRequest
//  }
  
}