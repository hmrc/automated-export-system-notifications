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

import org.mockito.Mockito.*
import org.apache.pekko.util.ByteString
import play.api.mvc.{AnyContentAsRaw, RawBuffer, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters.*

class ValidatedRequestActionSpec extends BaseSpec {
  trait Setup {
    when(mockAppConfig.eisToken).thenReturn("TEST-token")

    private val cc = stubControllerComponents()

    val validateRequestAction =
      new ValidatedRequestAction(cc.parsers, mockAppConfig)
  }
  "refine" - {
    "returns Unauthorized when Authorization header is missing" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Content-Type" -> "application/xml")
        .withBody("<xml/>")

      validateRequestAction.refine(request).futureValue shouldBe Left(Results.Unauthorized("Invalid Authorization header"))
    }

    "returns Unauthorized when Authorization header is invalid" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "wrong-token", "Content-Type" -> "application/xml")
        .withBody("<xml/>")

      validateRequestAction.refine(request).futureValue shouldBe Left(Results.Unauthorized("Invalid Authorization header"))
    }

    "returns BadRequest when body is missing" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "Bearer test-Token")

      validateRequestAction.refine(request).futureValue shouldBe Left(Results.BadRequest("Request body is required"))
    }

    "returns BadRequest when XML is invalid" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "Bearer test-token", "Content-Type" -> "application/xml")
        .withTextBody("<xml>")

      validateRequestAction.refine(request).futureValue shouldBe Left(Results.BadRequest("Invalid XML payload"))
    }

    "returns ValidatedRequest when XML is valid" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "Bearer Test-Token", "Content-Type" -> "application/xml")
        .withXmlBody(<root><value>abc</value></root>)

      validateRequestAction.refine(request).futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
      }
    }

    "logs the received payload and all headers except Authorization" in new Setup {
      val xml = "<root><value>abc</value></root>"

      val request =
        FakeRequest(POST, "/")
          .withHeaders(
            "Authorization"    -> "Bearer test-token",
            "Content-Type"     -> "text/xml",
            "x-correlation-id" -> "corr-123"
          )
          .withTextBody(xml)

      val actionLogger: Logger =
        LoggerFactory
          .getLogger(classOf[ValidatedRequestAction])
          .asInstanceOf[Logger]

      val originalLevel: Level = actionLogger.getLevel
      val listAppender = new ListAppender[ILoggingEvent]()

      listAppender.start()
      actionLogger.addAppender(listAppender)
      actionLogger.setLevel(Level.DEBUG)

      try {
        validateRequestAction
          .refine(request)
          .futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
        }

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

        logMessage should include(xml)

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

    "logs and accepts a raw payload larger than 100 KB" in new Setup {
      val largeValue = "x" * 150000

      val xml =
        s"""<?xml version="1.0" encoding="UTF-8"?><root   attr="x" ><value>$largeValue</value></root>"""

      val payloadBytes = ByteString(xml, "UTF-8")
      val rawBuffer    = mock[RawBuffer]

      when(rawBuffer.size)
        .thenReturn(payloadBytes.size.toLong)

      when(rawBuffer.asBytes(payloadBytes.size.toLong))
        .thenReturn(Some(payloadBytes))

      val request =
        FakeRequest(POST, "/")
          .withHeaders(
            "Authorization"    -> "Bearer test-token",
            "Content-Type"     -> "application/xml",
            "Content-Length"   -> payloadBytes.size.toString,
            "x-correlation-id" -> "corr-large-payload"
          )
          .withBody(AnyContentAsRaw(rawBuffer))

      val actionLogger: Logger =
        LoggerFactory
          .getLogger(classOf[ValidatedRequestAction])
          .asInstanceOf[Logger]

      val originalLevel: Level = actionLogger.getLevel
      val listAppender = new ListAppender[ILoggingEvent]()

      listAppender.start()
      actionLogger.addAppender(listAppender)
      actionLogger.setLevel(Level.DEBUG)

      try {
        validateRequestAction
          .refine(request)
          .futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
        }

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

        logMessage should include(xml)
        logMessage should include(
          "x-correlation-id=corr-large-payload"
        )

        logMessage.toLowerCase should not include "authorization"
        logMessage             should not include "test-token"

        verify(rawBuffer).asBytes(payloadBytes.size.toLong)
      } finally {
        actionLogger.detachAppender(listAppender)
        actionLogger.setLevel(originalLevel)
        listAppender.stop()
      }
    }

    "returns ValidatedRequest when body is text and valid XML" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "Bearer test-token", "Content-Type" -> "text/xml")
        .withTextBody("<root><value>abc</value></root>")

      validateRequestAction.refine(request).futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
      }
    }
  }
}
