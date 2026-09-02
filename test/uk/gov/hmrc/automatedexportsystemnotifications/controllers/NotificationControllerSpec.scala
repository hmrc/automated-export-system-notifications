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

import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.ValidatedRequestAction
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.*
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers.stubControllerComponents

import java.time.{Clock, Instant, ZoneOffset}
import play.api.mvc.BodyParsers
import uk.gov.hmrc.automatedexportsystemnotifications.models.errors.UnifiedErrorCode.*

import scala.concurrent.Future

class NotificationControllerSpec extends BaseSpec {

  trait Setup:

    when(mockAppConfig.eisToken).thenReturn("test-token")
    val fixedClock: Clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC)

    val cc                  = stubControllerComponents()
    private val bodyParsers = new BodyParsers.Default(cc.parsers)

    val validatedRequestAction: ValidatedRequestAction =
      new ValidatedRequestAction(bodyParsers, mockAppConfig)

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

    "return 204 when parse succeeds and service returns success" in new Setup {
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
        """<?xml version="1.0" encoding="UTF-8"?>
          |<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          | <Header>
          |    <messageSender>NECA.XI</messageSender>
          |    <messageRecipient>GB123456789000</messageRecipient>
          |    <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
          |    <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
          |    <messageType>ACK</messageType>
          |    <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          | </Header>
          | <Body>
          |    <messageCode>CC507C</messageCode>
          |    <actionCode>1</actionCode>
          |    <MRN>26GB123456789ABCDE1</MRN>
          | </Body>
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

    "return 204 when parse succeeds with multiple IE906 FunctionalErrors and service returns success" in new Setup {
      val xml =
        """<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          |<Header>
          |    <messageSender>NECA.XI</messageSender>
          |    <messageRecipient>GB123456789000</messageRecipient>
          |    <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
          |    <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
          |    <messageType>CD906C</messageType>
          |    <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          |</Header>
          |<Body>
          |    <messageCode>CC507C</messageCode>
          |    <MRN>26GB123456789ABCDE1</MRN>
          |    <FunctionalError>
          |      <errorPointer>Body.ExportOperation.MRN</errorPointer>
          |      <errorCode>90</errorCode>
          |      <errorReason>reason-1</errorReason>
          |      <originalAttributeValue>26GB123</originalAttributeValue>
          |    </FunctionalError>
          |
          |    <FunctionalError>
          |      <errorPointer> Body.GoodsShipment.Consignment.ReferenceNumberUCRID </errorPointer>
          |      <errorCode>96</errorCode>
          |      <errorReason>reason-2</errorReason>
          |      <originalAttributeValue>DUCR001</originalAttributeValue>
          |    </FunctionalError>
          |
          |    <FunctionalError>
          |      <errorPointer>Body.Unknown.Path </errorPointer>
          |      <errorCode>12345</errorCode>
          |      <errorReason>reason-3</errorReason>
          |    </FunctionalError>
          |  </Body>
          |</AESDigitalNotification>""".stripMargin

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

      val request =
        FakeRequest(POST, "/notifications")
          .withTextBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "test-token")

      val result = controller.notification(request)

      status(result) shouldBe NO_CONTENT

      val errorsCaptor: ArgumentCaptor[Option[List[NotificationError]]] =
        ArgumentCaptor.forClass(classOf[Option[List[NotificationError]]])

      verify(mockService, Mockito.atLeast(1)).sendNotification(
        any(),
        any(),
        any(),
        any(),
        errorsCaptor.capture()
      )(any())

      val sentErrors: List[NotificationError] = errorsCaptor.getValue.value

      sentErrors should have size 3

      sentErrors(0).code          shouldBe UnknownMrn.code
      sentErrors(0).path          shouldBe Some("Body.ExportOperation.MRN")
      sentErrors(0).originalValue shouldBe Some("26GB123")
      sentErrors(0).description   shouldBe Some(UnknownMrn.description)

      sentErrors(1).code          shouldBe DiversionRejectedInvalidDeclaration.code
      sentErrors(1).path          shouldBe Some("Body.GoodsShipment.Consignment.ReferenceNumberUCRID")
      sentErrors(1).originalValue shouldBe Some("DUCR001")
      sentErrors(1).description   shouldBe Some(DiversionRejectedInvalidDeclaration.description)

      sentErrors(2).code          shouldBe "UNKNOWN_ERROR"
      sentErrors(2).path          shouldBe Some("Body.Unknown.Path")
      sentErrors(2).originalValue shouldBe None
      sentErrors(2).description   shouldBe Some("Something went wrong")
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
        """<?xml version="1.0" encoding="UTF-8"?>
          |<AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          | <Header>
          |    <messageSender>NECA.XI</messageSender>
          |    <messageRecipient>GB123456789000</messageRecipient>
          |    <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
          |    <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
          |    <messageType>ACK</messageType>
          |    <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          | </Header>
          | <Body>
          |    <messageCode>CC507C</messageCode>
          |    <actionCode>1</actionCode>
          |    <MRN>26GB123456789ABCDE1</MRN>
          | </Body>
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
