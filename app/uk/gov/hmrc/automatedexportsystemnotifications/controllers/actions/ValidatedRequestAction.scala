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

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.automatedexportsystemnotifications.config.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.xml.XML

case class ValidatedRequest[A](request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class ValidatedRequestAction @Inject() (
  bodyParsers: PlayBodyParsers,
  appConfig:   AppConfig
)(implicit ec: ExecutionContext)
    extends ActionBuilder[ValidatedRequest, AnyContent]
    with ActionRefiner[Request, ValidatedRequest]
    with Logging {

  private val expectedAuthHeader: String = appConfig.eisToken

  override def parser: BodyParser[AnyContent] = bodyParsers.raw.map(rawBuffer => AnyContentAsRaw(rawBuffer))

  override protected def executionContext: ExecutionContext = ec

  override def refine[A](request: Request[A]): Future[Either[Result, ValidatedRequest[A]]] = {
    val headersToLog: Seq[(String, String)] =
      request.headers.headers.filterNot { case (name, _) =>
        name.equalsIgnoreCase("Authorization")
      }

    val maybePayload: Option[String] =
      request.body match {
        case any: AnyContent =>
          extractPayload(any)

        case _ =>
          None
      }

    logger.debug(
      s"Received notification from HMRC. " +
        s"Headers: ${headersToLog.map { case (name, value) => s"$name=$value" }.mkString(", ")}. " +
        s"Payload: ${maybePayload.getOrElse("")}"
    )

    val maybeAuth: Option[String] =
      request.headers.get("Authorization")

    if (maybeAuth.forall(_ != expectedAuthHeader)) {
      val warningMessage: String =
        if (maybeAuth.isEmpty)
          "Notification request rejected: missing authorization header"
        else
          "Notification request rejected: invalid authorization header"

      logger.warn(warningMessage)

      Future.successful(
        Left(Results.Unauthorized("Invalid Authorization header"))
      )
    } else {
      maybePayload match {
        case Some(payload) if payload.nonEmpty =>
          val isValidXml: Boolean =
            Try(XML.loadString(payload)).isSuccess

          if (!isValidXml) {
            logger.error("Invalid XML payload received")

            Future.successful(
              Left(Results.BadRequest("Invalid XML payload"))
            )
          } else {
            Future.successful(
              Right(ValidatedRequest(request))
            )
          }

        case _ =>
          logger.error("Missing request body")

          Future.successful(
            Left(Results.BadRequest("Request body is required"))
          )
      }
    }
  }

  private def extractPayload(any: AnyContent): Option[String] =
    any.asRaw
      .flatMap(_.asBytes().map(_.utf8String))
      .orElse(any.asText)
      .orElse(any.asXml.map(_.toString()))
}
