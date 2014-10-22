package controllers

case class NotFoundEx(message: String) extends RuntimeException(message)
