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

package uk.gov.hmrc.automatedexportsystemnotifications.xmlWriters

import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationPayload}

import java.time.format.DateTimeFormatter
import scala.xml.{Elem, NodeSeq}

object AesNotificationWriter {

  private val dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def toXml(payload: NotificationPayload): Either[String, Elem] =
    validate(payload).map { _ =>
      <notification>
        <correlationId>{payload.correlationId.toString}</correlationId>
        <dateCreated>{dtf.format(payload.dateCreated)}</dateCreated>
        <status>{payload.status.value.toString}</status>
        {
        if (payload.errors.nonEmpty) {
          <errors>
          {payload.errors.map(toErrorXml)}
        </errors>
        } else NodeSeq.Empty
      }
      </notification>
    }

  private def toErrorXml(err: NotificationError): Elem =
    <error>
      <code>{err.code}</code>
      {err.description.map(v => <description>{v}</description>).getOrElse(NodeSeq.Empty)}
      {err.path.map(v => <path>{v}</path>).getOrElse(NodeSeq.Empty)}
    </error>

  private def validate(payload: NotificationPayload): Either[String, Unit] =
    if (payload.errors.exists(_.code.trim.isEmpty))
      Left("Each error.code is required and must be non-empty")
    else
      Right(())
}
