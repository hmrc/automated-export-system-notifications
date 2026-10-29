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

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.read.ListAppender
import org.mockito.Mockito.*
import org.scalatest.EitherValues
import org.slf4j.LoggerFactory
import play.api.http.MimeTypes
import play.api.mvc.{Request, Result, Results}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.requests.NotificationsXmlPayloadRequest
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.{BaseSpec, XmlOps}

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.xml.{Elem, NodeSeq}

class CorrelatedPayloadActionRefinerSpec extends BaseSpec, EitherValues {
  trait Setup {
    when(mockAppConfig.eisToken).thenReturn("TEST-token")

    val validateRequestAction =
      new CorrelatedPayloadActionRefiner()
  }

  object TestData:
    val successfulBlockNodeSeq: Request[NodeSeq] => Future[Result] = _ =>
      Future.successful(
        Results.Ok("success")
      )
  end TestData

  "CorrelatedPayloadActionRefiner" - {
    ".invokeBlock" - {

      "should return a Result" - {

        "when the correlationId header is present" in new Setup {
          val xml: Elem =
            <root>
              <value>abc</value>
            </root>

          val request: FakeRequest[NodeSeq] =
            FakeRequest()
              .withHeaders(
                "Authorization"    -> "Bearer test-token",
                "x-correlation-id" -> "corr-123"
              )
              .withBody(xml)

          val notificationsXmlPayloadRequest: NotificationsXmlPayloadRequest[NodeSeq] =
            NotificationsXmlPayloadRequest(xml, request)

          val actionLogger: Logger =
            LoggerFactory
              .getLogger(classOf[CorrelatedPayloadActionRefiner])
              .asInstanceOf[Logger]

          val originalLevel: Level = actionLogger.getLevel
          val listAppender = new ListAppender[ILoggingEvent]()

          listAppender.start()
          actionLogger.addAppender(listAppender)
          actionLogger.setLevel(Level.DEBUG)

          try {
            val result: Future[Result] = validateRequestAction
              .invokeBlock(notificationsXmlPayloadRequest, TestData.successfulBlockNodeSeq)

            Helpers.status(result)          shouldBe Helpers.OK
            Helpers.contentAsString(result) shouldBe "success"

            val logMessage: String =
              listAppender.list.asScala
                .find(event =>
                  event.getLevel == Level.DEBUG &&
                    event.getFormattedMessage.startsWith(
                      "Received notification from HMRC"
                    )
                )
                .map(_.getFormattedMessage)
                .getOrElse(fail("Expected DEBUG log entry was not found"))

            logMessage should include(xml.toString)

            val expectedHeaders =
              request.headers.headers.filterNot { case (name, _) =>
                name.equalsIgnoreCase("Authorization")
              }

            expectedHeaders.foreach { case (name, value) =>
              logMessage should include(s"$name=$value")
            }

            logMessage.toLowerCase should not include "authorization"
            logMessage             should not include "test-token"
          } finally {
            actionLogger.detachAppender(listAppender)
            actionLogger.setLevel(originalLevel)
            listAppender.stop()
          }
        }

        "when the correlationId header is missing" in new Setup {
          val xml: Elem =
            <root>
              <value>abc</value>
            </root>

          val request: FakeRequest[NodeSeq] =
            FakeRequest()
              .withHeaders(
                "Authorization" -> "Bearer test-token"
              )
              .withBody(xml)

          val notificationsXmlPayloadRequest: NotificationsXmlPayloadRequest[NodeSeq] =
            NotificationsXmlPayloadRequest(xml, request)

          val result: Future[Result] = validateRequestAction
            .invokeBlock(notificationsXmlPayloadRequest, TestData.successfulBlockNodeSeq)

          val resultContent: String  = Helpers.contentAsString(result)
          val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

          val missingCorrelationIdHeaderErrorResponseXml: Elem =
            <NotificationsErrorResponse>
              <status>400</status>
              <message>Request X-Correlation-Id header is missing</message>
            </NotificationsErrorResponse>

          Helpers.status(result)      shouldBe Helpers.BAD_REQUEST
          Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
          XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(missingCorrelationIdHeaderErrorResponseXml)
        }
      }
    }
  }
}
