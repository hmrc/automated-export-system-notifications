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

package uk.gov.hmrc.automatedexportsystemnotifications.controllers.filters

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, EssentialFilter}
import uk.gov.hmrc.automatedexportsystemnotifications.config.BearerTokenConfig
import uk.gov.hmrc.automatedexportsystemnotifications.errors.{HeaderValidationError, InvalidAuthorizationHeader}
import uk.gov.hmrc.automatedexportsystemnotifications.models.http.CustomHeaderNames
import uk.gov.hmrc.automatedexportsystemnotifications.responses.NotificationsErrorResponse.toErrorResponse
import uk.gov.hmrc.automatedexportsystemnotifications.validation.{HeaderValidator, RequiredHeaderValidator}

import javax.inject.{Inject, Singleton}
import scala.util.matching.Regex

@Singleton
class RequiredAuthorizationFilter[T <: BearerTokenConfig] @Inject() (bearerTokenConfig: T) extends EssentialFilter, Logging:
  private def checkFormatAndCompare(headerValue: String): ValidatedNel[HeaderValidationError, Unit] =
    val bearerTokenRegex: Regex = "^Bearer\\s+(.+)$".r

    headerValue.trim match {
      case bearerTokenRegex(token) =>
        if token.equalsIgnoreCase(bearerTokenConfig.token) then ().validNel
        else {
          HeaderValidationError
            .InvalidHeader(
              HeaderNames.AUTHORIZATION,
              reason = None
            )
            .invalidNel
        }
      case _ =>
        HeaderValidationError
          .InvalidHeader(
            HeaderNames.AUTHORIZATION,
            reason = Some("Incorrect format")
          )
          .invalidNel
    }

  def apply(next: EssentialAction): EssentialAction =
    requestHeader =>
      val headerValidator: RequiredHeaderValidator =
        HeaderValidator(HeaderNames.AUTHORIZATION).required
          .checkUsing(checkFormatAndCompare)

      headerValidator
        .validate(requestHeader)
        .leftMap(errors =>
          val invalidAuthorizationHeader: InvalidAuthorizationHeader =
            InvalidAuthorizationHeader(errors.toList.map(_.message))

          val context: Seq[String] =
            Seq(
              s"Request Authorization header is invalid. " +
                s"reasons=[${invalidAuthorizationHeader.reasons.mkString(", ")}]"
            ) ++ requestHeader.headers
              .get(CustomHeaderNames.X_CORRELATION_ID)
              .map(value => s"correlationId=$value")

          logger.warn(context.mkString(" "))

          invalidAuthorizationHeader
        )
        .fold(
          error => Accumulator.done(error.toErrorResponse.toResult),
          _ => next(requestHeader)
        )
end RequiredAuthorizationFilter
