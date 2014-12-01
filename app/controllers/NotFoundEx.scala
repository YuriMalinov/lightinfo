package controllers

import play.api.UsefulException

class BaseUserEx(message: String) extends UsefulException(message) {
  title = message
  id = "#"
  description = message
}

case class NotFoundEx(message: String) extends BaseUserEx(message)

case class BadRequestEx(message: String) extends BaseUserEx(message)

case class ForbiddenEx(message: String) extends BaseUserEx(message)
