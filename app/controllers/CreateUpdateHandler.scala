package controllers

import play.api.mvc.{Action, AnyContent}

trait CreateUpdateHandler {
  def editImpl(id: Option[Int], save: Boolean): Action[AnyContent]

  def create() = editImpl(None, save = false)

  def createSave() = editImpl(None, save = true)

  def edit(id: Int) = editImpl(Some(id), save = false)

  def editSave(id: Int) = editImpl(Some(id), save = true)
}
