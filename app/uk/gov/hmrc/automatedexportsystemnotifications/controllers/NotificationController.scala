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

import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, RequestHeader}
import uk.gov.hmrc.automatedexportsystemnotifications.controllers.actions.ValidatedRequestAction
import uk.gov.hmrc.automatedexportsystemnotifications.models.aesRequest.{NotificationError, NotificationPayload, NotificationStatus}
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.{AckBody, IncomingPayload}
import uk.gov.hmrc.automatedexportsystemnotifications.parsers.EisPayloadXmlParser
import uk.gov.hmrc.automatedexportsystemnotifications.xmlWriters.AesNotificationWriter

import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//Validate request
//Log errors
//Response
//Create request for backend
//Call to back end service
//Log errors

@Singleton()
class NotificationController @Inject() (
    cc:              ControllerComponents,
    validatedRequestAction: ValidatedRequestAction
) (implicit ec: ExecutionContext)
 extends AbstractController(cc) {

  def handle: Action[AnyContent] = validatedRequestAction { req =>
    extractXmlString(req.body) match {
      case None =>
        Future.successful(BadRequest("Missing XML payload")) //convert to 400 response

      case Some(xmlString) =>
        EisPayloadXmlParser.parse(xmlString) match {
          case Left(parseErr) =>
            Future.successful(BadRequest(s"Invalid inbound payload: $parseErr"))

          case Right(incoming) =>
            val outbound = mapIncomingToNotification(incoming, req)

            AesNotificationWriter.toXml(outbound) match {
              case Left(writeErr) =>
                Future.successful(BadRequest(s"Could not build outbound XML: $writeErr"))

              case Right(outXml) =>
                // TODO: replace this with connector call to next system
                // connector.send(outXml.toString())

                Future.successful(
                  Ok(outXml.toString())
                    .as("application/xml")
                )
            }
        }
    }
  }

  private def extractXmlString(body: AnyContent): Option[String] =
  body.asXml.map(_.toString)
    .orElse(body.asText)
    .orElse(body.asRaw.flatMap(_.asBytes().map(_.utf8String)))

  private def mapIncomingToNotification(in: IncomingPayload, req: RequestHeader): NotificationPayload = {
    val correlationId =
      req.headers
        .get("x-correlation-id")
      in match {
      case IncomingPayload.Ack(a) =>
        val status = a.ActionCode match {
          case AckBody.ActionCodes.ACKNOWLEDGED_AND_PROCESSED => NotificationStatus.Accepted
          case AckBody.ActionCodes.DIVERSION => NotificationStatus.Diversion
          case _ => NotificationStatus.Rejected
        }

        NotificationPayload(
          correlationId = correlationId,
          dateCreated = OffsetDateTime.now(),
          status = Some(status),
          errors = Nil
        )

      case IncomingPayload.IE906(b) =>
        NotificationPayload(
          correlationId = correlationId.toString,
          dateCreated = OffsetDateTime.now(),
          status = Some(NotificationStatus.Rejected),
          errors = b.FunctionalError.map { e =>
            NotificationError(
              code = e.errorCode.toString, // or your unified constant
              description = Some(e.errorReason),
              path = Some(e.errorPointer)
            )
          }
        )

      case IncomingPayload.IE917(b) =>
        NotificationPayload(
          correlationId = correlationId,
          dateCreated = OffsetDateTime.now(),
          status = Some(NotificationStatus.Rejected),
          errors = b.XmlError.map { e =>
            NotificationError(
              code = e.errorCode.toString, // or your unified constant
              description = Some(e.errorText),
              path = Some(e.errorPointer)
            )
          }
        )
    }
  }
}