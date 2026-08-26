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

package uk.gov.hmrc.automatedexportsystemnotifications

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import uk.gov.hmrc.automatedexportsystemnotifications.connectors.AesConnector
import uk.gov.hmrc.automatedexportsystemnotifications.helpers.BaseSpec
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationStatus}
import uk.gov.hmrc.automatedexportsystemnotifications.services.AesService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class AesServiceSpec extends BaseSpec {

  private val fixedClock: Clock =
    Clock.fixed(Instant.parse("2026-08-11T12:00:00Z"), ZoneOffset.UTC)

  "AesService.sendNotification" - {

    "call connector.send and return Right when XML is built and connector succeeds" in {
      val mockConnector = mock[AesConnector]
      when(mockConnector.send(any())(any()))
        .thenReturn(Future.successful(Right(())))

      val service = new AesService(mockConnector, fixedClock)

      val result = service
        .sendNotification(
          correlationId = "corr-123",
          eori = "GB123456789000",
          mrn = "MRN123",
          status = NotificationStatus.Accepted,
          errors = None
        )
        .futureValue

      result shouldBe Right(())
      verify(mockConnector, times(1)).send(any[String])(any[HeaderCarrier])
    }

    "return Left and not call connector when XML writer validation fails" in {
      val mockConnector = mock[AesConnector]
      val service       = new AesService(mockConnector, fixedClock)

      val result = service
        .sendNotification(
          correlationId = "corr-123",
          eori = "GB123456789000",
          mrn = "MRN123",
          status = NotificationStatus.Rejected,
          errors = Some(List(NotificationError(code = "   ", description = None, path = None, originalValue = None)))
        )
        .futureValue

      result shouldBe Left("Failed to build AES notification XML: Each error.code is required and must be non-empty")
      verify(mockConnector, never()).send(any[String])(any[HeaderCarrier])
    }

    "propagate connector failure" in {
      val mockConnector = mock[AesConnector]
      when(mockConnector.send(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left("Expected 204, got 500. Body: boom")))

      val service = new AesService(mockConnector, fixedClock)

      val result = service
        .sendNotification(
          correlationId = "corr-123",
          eori = "GB123456789000",
          mrn = "MRN123",
          status = NotificationStatus.Accepted,
          errors = None
        )
        .futureValue

      result shouldBe Left("Expected 204, got 500. Body: boom")
    }
  }
}
