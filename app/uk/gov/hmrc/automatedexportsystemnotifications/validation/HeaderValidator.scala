package uk.gov.hmrc.automatedexportsystemnotifications.validation

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import play.api.mvc.RequestHeader
import uk.gov.hmrc.automatedexportsystemnotifications.errors.HeaderValidationError

sealed abstract class HeaderValidatorT[Self <: HeaderValidatorT[Self]](
  headerName: String
)(validation: ValidationRule[RequestHeader, HeaderValidationError, Unit]):
  self: Self =>

  def apply(newValidation: ValidationRule[RequestHeader, HeaderValidationError, Unit]): Self

  def validate(requestHeader: RequestHeader): ValidatedNel[HeaderValidationError, Unit] =
    validation.validate(requestHeader)

  def satisfies(p: String => Boolean, err: String => HeaderValidationError): Self =
    val validationRule: ValidationRule[RequestHeader, HeaderValidationError, Unit] =
      ValidationRule { request =>
        request.headers.get(headerName) match
          case None                    => ().validNel
          case Some(value) if p(value) => ().validNel
          case Some(_)                 => err(headerName).invalidNel
      }

    apply(validation.next(validationRule))

class HeaderValidator private (
  val headerName: String
)(validation: ValidationRule[RequestHeader, HeaderValidationError, Unit])
    extends HeaderValidatorT[HeaderValidator](headerName)(validation):
  def required: RequiredHeaderValidator =
    RequiredHeaderValidator(headerName)(validation)

  def apply(newValidation: ValidationRule[RequestHeader, HeaderValidationError, Unit]): HeaderValidator =
    new HeaderValidator(headerName)(newValidation)
end HeaderValidator

object HeaderValidator:
  def apply(headerName: String): HeaderValidator =
    new HeaderValidator(headerName)(ValidationRule(_ => ().validNel))

class RequiredHeaderValidator private[validation] (
  val headerName: String
)(validation: ValidationRule[RequestHeader, HeaderValidationError, Unit])
    extends HeaderValidatorT[RequiredHeaderValidator](headerName)(validation):
  private def requiredRule: ValidationRule[RequestHeader, HeaderValidationError, Unit] =
    ValidationRule { request =>
      request.headers.get(headerName) match
        case Some(value) => ().validNel
        case None        => HeaderValidationError.MissingRequiredHeader(headerName).invalidNel
    }

  def apply(newValidation: ValidationRule[RequestHeader, HeaderValidationError, Unit]): RequiredHeaderValidator =
    RequiredHeaderValidator(headerName)(newValidation)

  override def validate(requestHeader: RequestHeader): ValidatedNel[HeaderValidationError, Unit] =
    requiredRule.andThen(validation).validate(requestHeader)
