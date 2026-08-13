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

package uk.gov.hmrc.automatedexportsystemnotifications.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.ValidatedRequestAction
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.*
import uk.gov.hmrc.automatedexportsystemnotifications.services.AesService
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers.stubControllerComponents
import java.time.{Clock, Instant, ZoneOffset}
import play.api.mvc.{AnyContent, BodyParser}
import scala.concurrent.Future

class NotificationControllerSpec extends BaseSpec {

  trait Setup:
    val mockService: AesService = mock[AesService]
    when(mockAppConfig.eisToken).thenReturn("test-token")
    val fixedClock: Clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC)

    val cc = stubControllerComponents()
    private val parser: BodyParser[AnyContent] = cc.parsers.default

    val validatedRequestAction: ValidatedRequestAction =
      new ValidatedRequestAction(parser, mockAppConfig)

  "NotificationController.notification" - {

    "return 400 when xml payload is missing" in new Setup {
      val request = FakeRequest(POST, "/notifications")
        .withHeaders("Authorization" -> "test-token")

      val controller = new NotificationController(cc, validatedRequestAction, mockService, fixedClock)

      val result = controller.notification(request)

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "Request body is required"
      verifyNoInteractions(mockService)
    }

    "return 400 when inbound xml is invalid" in new Setup {
      val controller = new NotificationController(cc, validatedRequestAction, mockService, fixedClock)

      val badXml =
        """<AESDigitalNotification><Body><messageCode>CC507C</messageCode>"""
      val badRequest = FakeRequest(POST, "/notifications").withTextBody(badXml).withHeaders("Authorization" -> "test-token")

      val result = controller.notification(badRequest)

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Invalid XML payload")
      verifyNoInteractions(mockService)
    }

    "return 204 when parse succeeds and service returns Right" in new Setup {
      when(
        mockService.sendNotification(
          any[String],
          any[String],
          any[String],
          any[NotificationStatus],
          any[Option[List[NotificationError]]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Right(())))

      val controller = new NotificationController(cc, validatedRequestAction, mockService, fixedClock)

      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |    <messageSender>NECA.XI</messageSender>
          |    <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |    <messageCode>CC507C</messageCode>
          |    <actionCode>1</actionCode>
          |    <MRN>MRN123</MRN>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

      val request =
        FakeRequest(POST, "/notifications")
          .withTextBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "test-token")

      val result = controller.notification(request)

      status(result) shouldBe NO_CONTENT
      verify(mockService, times(1)).sendNotification(
        any[String],
        any[String],
        any[String],
        any[NotificationStatus],
        any[Option[List[NotificationError]]]
      )(any[HeaderCarrier])
    }

    "return 502 when service returns Left" in new Setup {

      when(
        mockService.sendNotification(
          any[String],
          any[String],
          any[String],
          any[NotificationStatus],
          any[Option[List[NotificationError]]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Left("downstream failed")))

      val controller = new NotificationController(cc, validatedRequestAction, mockService, fixedClock)

      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |  <Header>
          |    <messageSender>NECA.XI</messageSender>
          |    <messageRecipient>GB123456789000</messageRecipient>
          |  </Header>
          |  <Body>
          |    <messageCode>CC507C</messageCode>
          |    <actionCode>1</actionCode>
          |    <MRN>MRN123</MRN>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

      val request =
        FakeRequest(POST, "/notifications")
          .withTextBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "test-token")

      val result = controller.notification(request)

      status(result)          shouldBe BAD_GATEWAY
      contentAsString(result) shouldBe "Failed to call AES backend service"
    }
  }
}
