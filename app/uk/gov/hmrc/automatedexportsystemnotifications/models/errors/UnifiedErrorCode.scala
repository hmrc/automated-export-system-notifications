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

package uk.gov.hmrc.automatedexportsystemnotifications.models.errors

sealed trait UnifiedErrorCode {
  def code:        String
  def description: String
}

object UnifiedErrorCode:
  private val technicalError = "There is a problem with the service"
  case object InvalidFormatDucr extends UnifiedErrorCode {
    override val code        = "INVALID_FORMAT_DUCR"
    override val description = "Enter the DUCR in the correct format"
  }

  case object CodelistViolation extends UnifiedErrorCode {
    override val code        = "CODELIST_VIOLATION"
    override val description = technicalError
  }

  case object MissingConditionViolation extends UnifiedErrorCode {
    override val code        = "CONDITION_VIOLATION_MISSING"
    override val description = technicalError
  }

  case object RuleViolation extends UnifiedErrorCode {
    override val code        = "RULE_VIOLATION"
    override val description = technicalError
  }

  case object ConditionNotAllowedViolation extends UnifiedErrorCode {
    override val code        = "CONDITION_VIOLATION_NOT_ALLOWED"
    override val description = technicalError
  }

  case object DuplicatedMessageId extends UnifiedErrorCode {
    override val code        = "DUPLICATED_MESSAGE_ID"
    override val description = technicalError
  }

  case object RoleBaseAuthFailed extends UnifiedErrorCode {
    override val code        = "ROLEBASED_AUTH_FAILED"
    override val description = technicalError
  }

  case object MucrShut extends UnifiedErrorCode {
    override val code        = "MUCR_SHUT"
    override val description = "MUCR can no longer be updated"
  }

  case object TransitionalConstraintViolation extends UnifiedErrorCode {
    override val code        = "TRANSITIONAL_CONSTRAINT_VIOLATION"
    override val description = technicalError
  }

  case object EDIViolation extends UnifiedErrorCode {
    override val code        = "EDI_VIOLATION_POST_DOWNGRADE"
    override val description = technicalError
  }

  case object FunctionalViolation extends UnifiedErrorCode {
    override val code        = "FUNCTIONAL_VIOLATION_POST_DOWNGRADE"
    override val description = technicalError
  }

  case object UnknownMrn extends UnifiedErrorCode {
    override val code        = "UNKNOWN_MRN"
    override val description = "MRN not found"
  }

  case object MessageOutOfSequence extends UnifiedErrorCode {
    override val code        = "MESSAGE_OUT_OF_SEQUENCE"
    override val description = technicalError
  }

  case object InvalidMrn extends UnifiedErrorCode {
    override val code        = "INVALID_MRN"
    override val description = "MRN is not valid"
  }

  case object InvalidDiscrepancies extends UnifiedErrorCode {
    override val code        = "INVALID_DISCREPANCIES"
    override val description = "Discrepancies mismatch"
  }

  case object InvalidAmendment extends UnifiedErrorCode {
    override val code        = "INVALID_AMENDMENT"
    override val description = "Invalid amendment"
  }

  case object DiversionRejectedInvalidDeclaration extends UnifiedErrorCode {
    override val code        = "DIVERSION_REJECTED_INVALID_DECLARATION"
    override val description = "Invalid declaration"
  }

  case object DiversionRejectedUnknownMrn extends UnifiedErrorCode {
    override val code        = "DIVERSION_REJECTED_UNKNOWN_MRN"
    override val description = "Unknown MRN"
  }

  case object DiversionRejectedAlreadyExited extends UnifiedErrorCode {
    override val code        = "DIVERSION_REJECTED_ALREADY_EXITED"
    override val description = "Already exited"
  }

  case object DiversionRejectedOther extends UnifiedErrorCode {
    override val code        = "DIVERSION_REJECTED_OTHER"
    override val description = "Diversion rejected other"
  }

  case object MissingField extends UnifiedErrorCode {
    override val code        = "MISSING_FIELD"
    override val description = "Missing field"
  }

  case object Unknown extends UnifiedErrorCode {
    override val code        = "UNKNOWN_ERROR"
    override val description = "Something went wrong"
  }

final case class UnifiedErrorMetadata(
  unifiedCode:   UnifiedErrorCode,
  path:          Option[String],
  description:   String,
  originalValue: Option[String]
)
