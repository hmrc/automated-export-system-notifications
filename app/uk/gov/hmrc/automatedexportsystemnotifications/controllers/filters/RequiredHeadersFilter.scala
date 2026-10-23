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
                Some(
                  s"Missing required headers. " +
                    s"missingHeaders=[${errors.toList.map(_.headerName).mkString(", ")}]"
                ),
                requestHeader.headers
                  .get(CustomHeaderNames.X_CORRELATION_ID)
                  .map(value => s"correlationId=$value")
              ).flatten

            logger.warn(context.mkString(" "))

            HeaderFailedValidationError.apply(errors)
          )
          .fold(
            error => Accumulator.done(error.toErrorResponse.toResult),
            _ => next(requestHeader)
          )
