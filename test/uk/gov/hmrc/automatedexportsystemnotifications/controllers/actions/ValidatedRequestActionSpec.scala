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
import play.api.mvc.Results
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
    when(mockAppConfig.eisToken).thenReturn("test-token")

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
        .withHeaders("Authorization" -> "test-token")

      validateRequestAction.refine(request).futureValue shouldBe Left(Results.BadRequest("Request body is required"))
    }

    "returns BadRequest when XML is invalid" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "test-token", "Content-Type" -> "application/xml")
        .withTextBody("<xml>")

      validateRequestAction.refine(request).futureValue shouldBe Left(Results.BadRequest("Invalid XML payload"))
    }

    "returns ValidatedRequest when XML is valid" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "test-token", "Content-Type" -> "application/xml")
        .withXmlBody(<root><value>abc</value></root>)

      validateRequestAction.refine(request).futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
      }
    }

    "logs the received payload and all headers except Authorization" in new Setup {
      val xml = "<root><value>abc</value></root>"

      val request =
        FakeRequest(POST, "/")
          .withHeaders(
            "Authorization"    -> "test-token",
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

    "returns ValidatedRequest when body is text and valid XML" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "test-token", "Content-Type" -> "text/xml")
        .withTextBody("<root><value>abc</value></root>")

      validateRequestAction.refine(request).futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
      }
    }
  }
}
