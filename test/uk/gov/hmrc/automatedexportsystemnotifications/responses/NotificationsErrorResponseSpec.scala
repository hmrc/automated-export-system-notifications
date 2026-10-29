/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                NotificationsServiceHeaderValidationError("header1", "failed validation 1"),
                NotificationsServiceHeaderValidationError("header2", "failed validation 2"),
                NotificationsServiceHeaderValidationError("header3", "failed validation 3"),
                NotificationsServiceHeaderValidationError("header4", "failed validation 4"),
                NotificationsServiceHeaderValidationError("header5", "failed validation 5")
              )
            )

          val xml: Elem =
            <NotificationsErrorResponse>
              <status>412</status>
              <message>Request is BAD</message>
              <errors>
                <error>
                  <headerName>header1</headerName>
                  <message>failed validation 1</message>
                </error>
                <error>
                  <headerName>header2</headerName>
                  <message>failed validation 2</message>
                </error>
                <error>
                  <headerName>header3</headerName>
                  <message>failed validation 3</message>
                </error>
                <error>
                  <headerName>header4</headerName>
                  <message>failed validation 4</message>
                </error>
                <error>
                  <headerName>header5</headerName>
                  <message>failed validation 5</message>
                </error>
              </errors>
            </NotificationsErrorResponse>

          XmlOps.normalize(notificationsErrorResponse.toXml) shouldBe XmlOps.normalize(xml)
        }
      }
    }
  }
