import controllers.routes
import models.{AppDB, Bar}

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import org.squeryl.PrimitiveTypeMode.inTransaction

import play.api.http.ContentTypes.JSON
import play.api.test._
import play.api.test.Helpers._

class ApplicationSpec extends FlatSpec with ShouldMatchers {

  "A request to the addBar action" should "respond" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      val result = controllers.ApplicationController.addBar(FakeRequest().withFormUrlEncodedBody("name" -> "FooBar"))
      status(result) should equal (SEE_OTHER)
      redirectLocation(result) should equal (Some(routes.ApplicationController.index.url))
    }
  }
  
  "A request to the getBars Action" should "respond with data" in {
    running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
      inTransaction(AppDB.barTable insert Bar(Some("foo")))

      val result = controllers.ApplicationController.getBars(FakeRequest())
      status(result) should equal (OK)
      contentAsString(result) should include("foo")
    }
  }
  
}