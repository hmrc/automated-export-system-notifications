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

package uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import play.api.mvc.*
import play.api.mvc.Results.Status
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.XmlOps

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class NotificationsXmlPayloadActionRefinerSpec extends AnyFreeSpecLike, Matchers, EitherValues, DefaultAwaitTimeout:
  given ec: ExecutionContext = ExecutionContext.global

  object TestData:
    val correlationId = "correlationId"

    val successfulBlockNodeSeq: Request[NodeSeq] => Future[Result] = _ =>
      Future.successful(
        Status(Helpers.OK)("success")
      )

    val successfulBlockAnyContent: Request[AnyContent] => Future[Result] = _ =>
      Future.successful(
        Status(Helpers.OK)("success")
      )
  end TestData

  val notificationsXmlPayloadActionRefiner: NotificationsXmlPayloadActionRefiner =
    NotificationsXmlPayloadActionRefiner()

  "NotificationsXmlPayloadActionRefiner" - {

    "should return a Result" - {

      "when the body of the request is XML (NodeSeq)" in {
        val xml: Elem =
          <xml>xml</xml>

        val request: FakeRequest[NodeSeq] =
          FakeRequest(Helpers.GET, "/dummy/path")
            .withBody(xml)

        val result: Future[Result] =
          notificationsXmlPayloadActionRefiner.invokeBlock(
            request,
            TestData.successfulBlockNodeSeq
          )

        Helpers.status(result)          shouldBe Helpers.OK
        Helpers.contentAsString(result) shouldBe "success"
      }

      "when the body of the request is XML (AnyContent)" in {
        val xml: Elem =
          <xml>xml</xml>

        val request: FakeRequest[AnyContentAsXml] =
          FakeRequest()
            .withXmlBody(xml)

        val result: Future[Result] =
          notificationsXmlPayloadActionRefiner.invokeBlock(
            request,
            TestData.successfulBlockAnyContent
          )

        Helpers.status(result)          shouldBe Helpers.OK
        Helpers.contentAsString(result) shouldBe "success"
      }

      "when the body of the request is not XML" in {
        val text: String = "<element>I'm XML in disguise</element>"

        val request: FakeRequest[AnyContentAsText] =
          FakeRequest()
            .withTextBody(text)

        val result: Future[Result] =
          notificationsXmlPayloadActionRefiner.invokeBlock(
            request,
            TestData.successfulBlockAnyContent
          )

        val expectedXmlBodyErrorResponseXml: Elem =
          <NotificationsErrorResponse>
            <status>415</status>
            <message>The body of the request is not valid XML</message>
          </NotificationsErrorResponse>

        val resultContent: String  = Helpers.contentAsString(result)
        val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

        Helpers.status(result)      shouldBe Helpers.UNSUPPORTED_MEDIA_TYPE
        Helpers.contentType(result) shouldBe Some(Helpers.XML)
        XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(expectedXmlBodyErrorResponseXml)
      }
    }
  }
