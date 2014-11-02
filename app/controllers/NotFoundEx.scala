package controllers

case class NotFoundEx(message: String) extends RuntimeException(message)

case class BadRequestEx(message: String) extends RuntimeException(message)

case class ForbiddenEx(message: String) extends RuntimeException(message)
