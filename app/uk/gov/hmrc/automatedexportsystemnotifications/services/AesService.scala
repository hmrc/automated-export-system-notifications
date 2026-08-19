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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.automatedexportsystemnotifications.connectors.AesConnector
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationPayload, NotificationStatus}
import uk.gov.hmrc.automatedexportsystemnotifications.xmlWriters.*

import java.time.{Clock, OffsetDateTime, ZoneOffset}
import scala.concurrent.Future

@Singleton
class AesService @Inject() (
  connector: AesConnector,
  clock:     Clock
) {

  def sendNotification(
    correlationId: String,
    eori:          String,
    mrn:           String,
    status:        NotificationStatus,
    errors:        Option[List[NotificationError]]
  )(implicit hc: HeaderCarrier): Future[Either[String, Unit]] = {

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
        Future.successful(Left(s"Failed to build AES notification XML: $err"))

      case Right(xmlElem) =>
        connector.send(xmlElem.toString())
    }
  }
}
