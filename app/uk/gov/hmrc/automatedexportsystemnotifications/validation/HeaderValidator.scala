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

  def check(p: String => Boolean, err: String => HeaderValidationError): Self =
    val validationRule: ValidationRule[RequestHeader, HeaderValidationError, Unit] =
      ValidationRule { request =>
        request.headers.get(headerName) match
          case None                    => ().validNel
          case Some(value) if p(value) => ().validNel
          case Some(_)                 => err(headerName).invalidNel
      }

    apply(validation.next(validationRule))

  def checkUsing(f: String => ValidatedNel[HeaderValidationError, Unit]): Self =
    val validationRule: ValidationRule[RequestHeader, HeaderValidationError, Unit] =
      ValidationRule { request =>
        request.headers.get(headerName) match
          case None        => ().validNel
          case Some(value) => f(value)
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
