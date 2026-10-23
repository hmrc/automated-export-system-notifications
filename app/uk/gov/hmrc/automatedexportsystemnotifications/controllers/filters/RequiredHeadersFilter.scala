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

import cats.data.{NonEmptyList, Validated}
import play.api.Logging
import play.api.libs.streams.Accumulator
import play.api.mvc.EssentialFilter
import uk.gov.hmrc.automatedexportsystemnotifications.errors.HeaderFailedValidationError
import uk.gov.hmrc.automatedexportsystemnotifications.models.http.CustomHeaderNames
import uk.gov.hmrc.automatedexportsystemnotifications.responses.NotificationsErrorResponse.toErrorResponse
import uk.gov.hmrc.automatedexportsystemnotifications.validation.RequiredHeaderValidator

import javax.inject.Singleton

@Singleton
class RequiredHeadersFilter extends Logging:
  def apply(headerValidators: NonEmptyList[RequiredHeaderValidator]): EssentialFilter =
    next =>
      requestHeader =>
        headerValidators
          .traverse(_.validate(requestHeader))
          .leftMap(errors =>
            val context: Seq[String] =
              Seq(
                s"Missing required headers. " +
                  s"missingHeaders=[${errors.toList.map(_.headerName).mkString(", ")}]"
              ) ++ requestHeader.headers
                .get(CustomHeaderNames.X_CORRELATION_ID)
                .map(value => s"correlationId=$value")

            logger.warn(context.mkString(" "))

            HeaderFailedValidationError.apply(errors)
          )
          .fold(
            error => Accumulator.done(error.toErrorResponse.toResult),
            _ => next(requestHeader)
          )
