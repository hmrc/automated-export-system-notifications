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

package uk.gov.hmrc.automatedexportsystemnotifications.services

import cats.data.EitherT
import play.api.Logging
import play.api.http.Status
import uk.gov.hmrc.automatedexportsystemnotifications.connectors.AesConnector
import uk.gov.hmrc.automatedexportsystemnotifications.errors.AesServiceError
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationPayload, NotificationStatus}
import uk.gov.hmrc.automatedexportsystemnotifications.xml.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, OffsetDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AesService @Inject() (
  connector: AesConnector,
  clock:     Clock
)(using ExecutionContext)
    extends Logging {

  def sendNotification(
    correlationId: String,
    eori:          String,
    mrn:           String,
    status:        NotificationStatus,
    errors:        Option[List[NotificationError]]
  )(implicit hc: HeaderCarrier): EitherT[Future, AesServiceError, Unit] = {

    val payload = NotificationPayload(
      correlationId = correlationId,
      eori = eori,
      mrn = mrn,
      dateCreated = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC),
      status = status,
      errors = errors
    )

    AesNotificationWriter.toXml(payload) match {
      case Left(err) =>
        EitherT(
          Future.successful(
            Left(
              AesServiceError.NotificationFailedError(
                s"Failed to build AES notification XML: $err",
                Status.INTERNAL_SERVER_ERROR
              )
            )
          )
        )

      case Right(xmlElem) =>
        connector
          .send(xmlElem.toString(), correlationId)
          .leftMap(err =>
            AesServiceError.NotificationFailedError(
              s"Failed to send notification to AES",
              err.status
            )
          )
    }
  }
}
