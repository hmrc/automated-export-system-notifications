package uk.gov.hmrc.automatedexportsystemnotifications.responses

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.XmlOps

import scala.xml.Elem

class NotificationsErrorResponseSpec extends AnyFreeSpecLike, Matchers:
  "NotificationsErrorResponse" - {

    "should be able to be serialized to XML" - {

      "when there are no errors" in {
        val notificationsErrorResponse: NotificationsErrorResponse =
          NotificationsErrorResponse(123, "Request is BAD", errors = Nil)

        val xml: Elem =
          <NotificationsErrorResponse>
            <status>123</status>
            <message>Request is BAD</message>
          </NotificationsErrorResponse>

        XmlOps.normalize(notificationsErrorResponse.toXml) shouldBe XmlOps.normalize(xml)
      }

      "when there are errors" - {

        "NotificationsServiceValidationError" in {
          val notificationsErrorResponse: NotificationsErrorResponse =
            NotificationsErrorResponse(
              412,
              "Request is BAD",
              List(
                NotificationsServiceValidationError("failed validation 1"),
                NotificationsServiceValidationError("failed validation 2"),
                NotificationsServiceValidationError("failed validation 3"),
                NotificationsServiceValidationError("failed validation 4"),
                NotificationsServiceValidationError("failed validation 5")
              )
            )

          val xml: Elem =
            <NotificationsErrorResponse>
              <status>412</status>
              <message>Request is BAD</message>
              <errors>
                <error>
                  <message>failed validation 1</message>
                </error>
                <error>
                  <message>failed validation 2</message>
                </error>
                <error>
                  <message>failed validation 3</message>
                </error>
                <error>
                  <message>failed validation 4</message>
                </error>
                <error>
                  <message>failed validation 5</message>
                </error>
              </errors>
            </NotificationsErrorResponse>

          XmlOps.normalize(notificationsErrorResponse.toXml) shouldBe XmlOps.normalize(xml)
        }
      }
    }
  }
