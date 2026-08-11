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

import play.api.mvc.{Request, WrappedRequest}

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.Logging
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.XML
import scala.util.Try

case class ValidatedRequest[A](request: Request[A]) extends WrappedRequest[A](request)

@Singleton
class ValidatedRequestAction @Inject() (
  val parser: BodyParsers.Default,
  config:     Configuration
)(implicit ec: ExecutionContext)
    extends ActionBuilder[ValidatedRequest, AnyContent]
    with ActionRefiner[Request, ValidatedRequest]
    with Logging {

  private val expectedAuthHeader: String =
    config.getOptional[String]("auth.expectedAuthorizationHeader").getOrElse("")

  override def executionContext: ExecutionContext = ec

  override protected def refine[A](request: Request[A]): Future[Either[Result, ValidatedRequest[A]]] = {
    val maybeAuth = request.headers.get("Authorization")
    if (maybeAuth.forall(_ != expectedAuthHeader)) { // TODO: get auth header from config
      logger.warn(s"Unauthorized request. Provided Authorization header: ${maybeAuth.getOrElse("<missing>")}")
      Future.successful(Left(Results.Unauthorized("Invalid Authorization header")))
    } else {
      request.body match {
        case any: AnyContent =>
          val maybeXmlString =
            any.asXml
              .map(_.toString())
              .orElse(any.asText)
              .orElse(any.asRaw.flatMap(_.asBytes().map(_.utf8String)))

          maybeXmlString match {
            case Some(xmlString) =>
              val isValidXml = Try(XML.loadString(xmlString)).isSuccess
              if (!isValidXml) {
                logger.error("Invalid XML payload received")
                Future.successful(Left(Results.BadRequest("Invalid XML payload")))
              } else {
                Future.successful(Right(ValidatedRequest(request)))
              }

            case None =>
              logger.error("Missing request body")
              Future.successful(Left(Results.BadRequest("Request body is required")))
          }

        case _ =>
          logger.error("Unsupported request body type")
          Future.successful(Left(Results.BadRequest("Unsupported body type")))
      }
    }
  }
}
