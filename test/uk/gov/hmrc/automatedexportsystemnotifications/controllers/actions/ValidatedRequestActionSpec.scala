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
import play.api.mvc.{BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec

class ValidatedRequestActionSpec extends BaseSpec {
  trait Setup {
    when(mockAppConfig.eisToken).thenReturn("test-token")

    private val cc            = stubControllerComponents()
    private val bodyParsers   = new BodyParsers.Default(cc.parsers)
    val validateRequestAction = new ValidatedRequestAction(bodyParsers, mockAppConfig)
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

    "returns ValidatedRequest when body is text and valid XML" in new Setup {
      val request = FakeRequest(POST, "/")
        .withHeaders("Authorization" -> "test-token", "Content-Type" -> "text/xml")
        .withTextBody("<root><value>abc</value></root>")

      validateRequestAction.refine(request).futureValue should matchPattern { case Right(ValidatedRequest(_)) =>
      }
    }
  }
}
