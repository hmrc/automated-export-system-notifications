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

import cats.data.EitherT
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.EitherValues
import play.api.http.MimeTypes
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.automatedexportsystemnotifications.config.EisBearerTokenConfig
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.{CorrelatedPayloadActionRefiner, NotificationsXmlPayloadActionRefiner}
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.filters.{RequiredAuthorizationFilter, RequiredHeadersFilter}
import uk.gov.hmrc.automatedexportsystemnotifications.errors.AesServiceError
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.{BaseSpec, XmlOps}
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future
import scala.xml.{Elem, NodeSeq}

class NotificationControllerSpec extends BaseSpec, EitherValues {

  trait Setup:
    clearInvocations(mockService)

    when(mockAppConfig.eisToken).thenReturn("test-token")

    val system: ActorSystem = ActorSystem()

    given materializer: Materializer = Materializer(system)

    val fixedClock: Clock = Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC)

    val cc = stubControllerComponents()

    val notificationsXmlPayloadActionRefiner: NotificationsXmlPayloadActionRefiner =
      NotificationsXmlPayloadActionRefiner()

    val correlatedPayloadActionRefiner: CorrelatedPayloadActionRefiner =
      new CorrelatedPayloadActionRefiner()

    val eisBearerTokenConfig: EisBearerTokenConfig = EisBearerTokenConfig(mockAppConfig)

    val requiredHeadersFilter: RequiredHeadersFilter = RequiredHeadersFilter()

    val requiredAuthorizationFilter: RequiredAuthorizationFilter[EisBearerTokenConfig] =
      RequiredAuthorizationFilter(eisBearerTokenConfig)

    val controller: NotificationController =
      new NotificationController(
        cc,
        notificationsXmlPayloadActionRefiner,
        correlatedPayloadActionRefiner,
        requiredHeadersFilter,
        requiredAuthorizationFilter,
        mockService,
        fixedClock
      )
  "NotificationController.notification" - {

    "return 204 when parse succeeds and service returns success" in new Setup {
      when(
        mockService.sendNotification(
          any[String],
          any[String],
          any[String],
          any[NotificationStatus],
          any[Option[List[NotificationError]]]
        )(any[HeaderCarrier])
      ).thenReturn(EitherT(Future.successful(Right(()))))

      val xml: Elem =
        <AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
         <Header>
            <messageSender>NECA.XI</messageSender>
            <messageRecipient>GB123456789000</messageRecipient>
            <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
            <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
            <messageType>ACK</messageType>
            <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
         </Header>
         <Body>
            <messageCode>CC507C</messageCode>
            <actionCode>1</actionCode>
            <MRN>26GB123456789ABCDE1</MRN>
         </Body>
        </AESDigitalNotification>

      val request =
        FakeRequest(POST, "/notifications")
          .withBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "Bearer test-token")

      val result = Helpers.call(controller.notification, request)

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
      val xml: Elem =
        <AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          <Header>
              <messageSender>NECA.XI</messageSender>
              <messageRecipient>GB123456789000</messageRecipient>
              <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
              <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
              <messageType>CD906C</messageType>
              <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          </Header>
          <Body>
              <messageCode>CC507C</messageCode>
              <MRN>26GB123456789ABCDE1</MRN>
              <FunctionalError>
                <errorPointer>Body.ExportOperation.MRN</errorPointer>
                <errorCode>90</errorCode>
                <errorReason>reason-1</errorReason>
                <originalAttributeValue>26GB123</originalAttributeValue>
              </FunctionalError>
              <FunctionalError>
                <errorPointer> Body.GoodsShipment.Consignment.ReferenceNumberUCRID </errorPointer>
                <errorCode>96</errorCode>
                <errorReason>reason-2</errorReason>
                <originalAttributeValue>DUCR001</originalAttributeValue>
              </FunctionalError>
              <FunctionalError>
                <errorPointer>Body.Unknown.Path </errorPointer>
                <errorCode>12345</errorCode>
                <errorReason>reason-3</errorReason>
              </FunctionalError>
            </Body>
          </AESDigitalNotification>

      val request =
        FakeRequest(POST, "/notifications")
          .withBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "Bearer test-token")

      val result = Helpers.call(controller.notification, request)

      status(result) shouldBe NO_CONTENT

      when(
        mockService.sendNotification(
          ArgumentMatchers.eq("corr-123"),
          ArgumentMatchers.eq("GB123456789000"),
          ArgumentMatchers.eq("26GB123456789ABCDE1"),
          ArgumentMatchers.eq(NotificationStatus.Rejected),
          ArgumentMatchers.eq(
            Some(
              List(
                NotificationError(
                  code = "UNKNOWN_MRN",
                  description = Some("MRN not found"),
                  path = Some("Body.ExportOperation.MRN"),
                  originalValue = Some("26GB123")
                ),
                NotificationError(
                  code = "DIVERSION_REJECTED_INVALID_DECLARATION",
                  description = Some("Invalid declaration"),
                  path = Some("Body.GoodsShipment.Consignment.ReferenceNumberUCRID"),
                  originalValue = Some("DUCR001")
                ),
                NotificationError(
                  code = "UNKNOWN_ERROR",
                  description = Some("Something went wrong"),
                  path = Some("Body.Unknown.Path"),
                  originalValue = None
                )
              )
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(EitherT(Future.successful(Right(()))))
    }

    "return 204 when parse succeeds with multiple IE917 XmlErrors and service returns success" in new Setup {
      val xml: Elem =
        <AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          <Header>
              <messageSender>NECA.XI</messageSender>
              <messageRecipient>GB123456789000</messageRecipient>
              <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
              <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
              <messageType>CD917C</messageType>
              <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          </Header>
          <Body>
              <messageCode>CD917C</messageCode>
              <MRN>26GB123456789ABCDE1</MRN>
              <XmlError>
                <errorPointer>Body.ExportOperation.MRN</errorPointer>
                <errorCode>13</errorCode>
                <errorText>reason-1</errorText>
                <originalAttributeValue>26GB123</originalAttributeValue>
              </XmlError>
              <XmlError>
                <errorPointer>Body.GoodsShipment.Consignment.ReferenceNumberUCRID</errorPointer>
                <errorCode>15</errorCode>
                <errorText>reason-2</errorText>
                <originalAttributeValue>DUCR001</originalAttributeValue>
              </XmlError>
              <XmlError>
                <errorPointer>Body.Unknown.Path</errorPointer>
                <errorCode>12345</errorCode>
                <errorText>reason-3</errorText>
              </XmlError>
            </Body>
          </AESDigitalNotification>

      val request: FakeRequest[NodeSeq] =
        FakeRequest(POST, "/notifications")
          .withBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "Bearer test-token")

      when(
        mockService.sendNotification(
          ArgumentMatchers.eq("corr-123"),
          ArgumentMatchers.eq("GB123456789000"),
          ArgumentMatchers.eq("26GB123456789ABCDE1"),
          ArgumentMatchers.eq(NotificationStatus.Rejected),
          ArgumentMatchers.eq(
            Some(
              List(
                NotificationError(
                  code = "MISSING",
                  description = Some("A mandatory field has not been provided."),
                  path = Some("Body.ExportOperation.MRN"),
                  originalValue = Some("26GB123")
                ),
                NotificationError(
                  code = "NOT_SUPPORTED_IN_POSITION",
                  description = Some("The field or value is not valid in this part of the message."),
                  path = Some("Body.GoodsShipment.Consignment.ReferenceNumberUCRID"),
                  originalValue = Some("DUCR001")
                ),
                NotificationError(
                  code = "UNKNOWN_ERROR",
                  description = Some("Something went wrong"),
                  path = Some("Body.Unknown.Path"),
                  originalValue = None
                )
              )
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(EitherT(Future.successful(Right(()))))

      val result = Helpers.call(controller.notification, request)

      status(result) shouldBe NO_CONTENT
    }

    "return 400 when correlationId header is missing" in new Setup {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, "/notifications")
          .withHeaders("Authorization" -> "Bearer test-token")

      val result: Future[Result] = Helpers.call(controller.notification, request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val headerFailedValidationErrorResponseXml: Elem =
        <NotificationsErrorResponse>
          <status>400</status>
          <message>Header validation failed</message>
          <errors>
            <error>
              <headerName>X-Correlation-Id</headerName>
              <message>Missing required header</message>
            </error>
          </errors>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.BAD_REQUEST
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(headerFailedValidationErrorResponseXml)
    }

    "return 401 when authorization fails" in new Setup {
      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, "/notifications")
          .withHeaders(
            "Authorization"    -> "Bearer not-correct-token",
            "x-correlation-id" -> "correlationId"
          )

      val result: Future[Result] = Helpers.call(controller.notification, request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val invalidAuthorizationHeaderErrorResponseXml: Elem =
        <NotificationsErrorResponse>
          <status>401</status>
          <message>Request Authorization header is invalid. Invalid header.</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.UNAUTHORIZED
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(invalidAuthorizationHeaderErrorResponseXml)
    }

    "return 415 when xml payload is missing" in new Setup {
      val request = FakeRequest(POST, "/notifications")
        .withHeaders(
          "Authorization"    -> "Bearer test-token",
          "x-correlation-id" -> "correlationId"
        )

      val result: Future[Result] = Helpers.call(controller.notification, request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val expectedXmlBodyErrorResponseXml: Elem =
        <NotificationsErrorResponse>
          <status>415</status>
          <message>The body of the request is not valid XML</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.UNSUPPORTED_MEDIA_TYPE
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(expectedXmlBodyErrorResponseXml)
    }

    "return 415 when payload is not valid xml" in new Setup {
      val request = FakeRequest(POST, "/notifications")
        .withHeaders(
          "Authorization"    -> "Bearer test-token",
          "x-correlation-id" -> "correlationId"
        )
        .withBody("non-xml-body")

      val result: Future[Result] = Helpers.call(controller.notification, request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val expectedXmlBodyErrorXml: Elem =
        <NotificationsErrorResponse>
          <status>415</status>
          <message>The body of the request is not valid XML</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.UNSUPPORTED_MEDIA_TYPE
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(expectedXmlBodyErrorXml)
    }

    "return 500 when service returns a 500 error" in new Setup {

      when(
        mockService.sendNotification(
          any[String],
          any[String],
          any[String],
          any[NotificationStatus],
          any[Option[List[NotificationError]]]
        )(any[HeaderCarrier])
      ).thenReturn(
        EitherT(
          Future.successful(
            Left(
              AesServiceError.NotificationFailedError(
                "Notification failed",
                Helpers.INTERNAL_SERVER_ERROR
              )
            )
          )
        )
      )

      val xml: Elem =
        <AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
         <Header>
            <messageSender>NECA.XI</messageSender>
            <messageRecipient>GB123456789000</messageRecipient>
            <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
            <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
            <messageType>ACK</messageType>
            <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
         </Header>
         <Body>
            <messageCode>CC507C</messageCode>
            <actionCode>1</actionCode>
            <MRN>26GB123456789ABCDE1</MRN>
         </Body>
        </AESDigitalNotification>

      val request =
        FakeRequest(POST, "/notifications")
          .withBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "Bearer test-token")

      val result = Helpers.call(controller.notification, request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val notificationFailedErrorResponseXml: Elem =
        <NotificationsErrorResponse>
          <status>500</status>
          <message>Notification failed</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.INTERNAL_SERVER_ERROR
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(notificationFailedErrorResponseXml)
    }

    "return 502 when service returns a 502 error" in new Setup {

      when(
        mockService.sendNotification(
          any[String],
          any[String],
          any[String],
          any[NotificationStatus],
          any[Option[List[NotificationError]]]
        )(any[HeaderCarrier])
      ).thenReturn(
        EitherT(
          Future.successful(
            Left(
              AesServiceError.NotificationFailedError(
                "Notification failed",
                Helpers.BAD_GATEWAY
              )
            )
          )
        )
      )

      val xml: Elem =
        <AESDigitalNotification xmlns="http://www.hmrc.gsi.gov.uk/eis">
          <Header>
            <messageSender>NECA.XI</messageSender>
            <messageRecipient>GB123456789000</messageRecipient>
            <preparationDateTime>2026-07-21T10:00:00</preparationDateTime>
            <messageIdentification>f50929c4-39f5-4f33-8172-77a22588d</messageIdentification>
            <messageType>ACK</messageType>
            <correlationIdentifier>f50929c4-39f5-4f33-8172-77a22588d</correlationIdentifier>
          </Header>
          <Body>
            <messageCode>CC507C</messageCode>
            <actionCode>1</actionCode>
            <MRN>26GB123456789ABCDE1</MRN>
          </Body>
        </AESDigitalNotification>

      val request =
        FakeRequest(POST, "/notifications")
          .withBody(xml)
          .withHeaders("x-correlation-id" -> "corr-123", "Authorization" -> "Bearer test-token")

      val result = Helpers.call(controller.notification, request)

      val resultContent: String  = Helpers.contentAsString(result)
      val resultXml:     NodeSeq = XmlOps.loadXmlFromString(resultContent).value

      val notificationFailedErrorResponseXml: Elem =
        <NotificationsErrorResponse>
          <status>502</status>
          <message>Notification failed</message>
        </NotificationsErrorResponse>

      Helpers.status(result)      shouldBe Helpers.BAD_GATEWAY
      Helpers.contentType(result) shouldBe Some(MimeTypes.XML)
      XmlOps.normalize(resultXml) shouldBe XmlOps.normalize(notificationFailedErrorResponseXml)
    }
  }
}
