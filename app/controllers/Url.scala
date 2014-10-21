package controllers

import java.net.URLEncoder

import play.api.mvc.Request

object Url {
  def apply(parts: (Symbol, Any)*)(implicit request: Request[_]) = {
    val newArgs = parts.map(p ⇒ p._1.name → List(p._2.toString)).toMap

    request.path + "?" + (request.queryString ++ newArgs).flatMap { case (name, value) ⇒
        value.map (v ⇒ URLEncoder.encode(name, "utf-8") + "=" + URLEncoder.encode(v, "utf-8"))
      } . mkString("&")
  }
}
