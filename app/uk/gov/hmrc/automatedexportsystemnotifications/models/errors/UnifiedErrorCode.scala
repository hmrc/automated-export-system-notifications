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

  case object IncorrectEnumaration extends UnifiedErrorCode {
    override val code        = "INCORRECT_ENUMERATION"
    override val description = "A field contains a value that is not permitted from the allowed list."
  }

  case object MissingField extends UnifiedErrorCode {
    override val code        = "MISSING"
    override val description = "A mandatory field has not been provided."
  }

  case object NotSupportedInPosition extends UnifiedErrorCode {
    override val code        = "NOT_SUPPORTED_IN_POSITION"
    override val description = "The field or value is not valid in this part of the message."
  }

  case object UnspecifiedErrorOther extends UnifiedErrorCode {
    override val code        = "UNSPECIFIED_ERROR_OTHER"
    override val description = "An unexpected error occurred that does not match a specific validation category."
  }

  case object TooManyRepetitions extends UnifiedErrorCode {
    override val code        = "TOO_MANY_REPETITIONS"
    override val description = "Too many occurrences of the same element were provided."
  }

  case object ElementTooLong extends UnifiedErrorCode {
    override val code        = "ELEMENT_TOO_LONG"
    override val description = "A field contains more characters than allowed."
  }

  case object ElementTooShort extends UnifiedErrorCode {
    override val code        = "ELEMENT_TOO_SHORT"
    override val description = "A field contains fewer characters than required."
  }

  case object InvalidValueSpecificType extends UnifiedErrorCode {
    override val code        = "INVALID_VALUE_SPECIFICTYPE"
    override val description = "The value provided is not valid for the expected data type."
  }

  case object InvalidValuePattern extends UnifiedErrorCode {
    override val code        = "INVALID_VALUE_PATTERN"
    override val description = "The value does not match the required format or pattern."
  }

  case object InvalidXmlFormat extends UnifiedErrorCode {
    override val code        = "INVALID_XML_FORMAT"
    override val description = "The message XML structure is invalid and cannot be processed."
  }

  case object InvalidCharacters extends UnifiedErrorCode {
    override val code        = "INVALID_CHARACTERS"
    override val description = "The message contains unsupported or invalid characters."
  }

  case object ValueLowerAllowedLowestLimit extends UnifiedErrorCode {
    override val code        = "VALUE_LOWER_ALLOWED_LOWEST_LIMIT"
    override val description = "The value is below the minimum allowed limit."
  }

  case object ValueGreaterAllowedUpperLimit extends UnifiedErrorCode {
    override val code        = "VALUE_GREATER_ALLOWED_UPPER_LIMIT"
    override val description = "The value exceeds the maximum allowed limit."
  }

  case object ValueLowerOrEqualAllowedLowestLimit extends UnifiedErrorCode {
    override val code        = "VALUE_LOWER_OR_EQUAL_ALLOWED_LOWEST_LIMIT"
    override val description = "The value must be greater than the minimum allowed limit."
  }

  case object ValueGreaterOrEqualAllowedLowestLimit extends UnifiedErrorCode {
    override val code        = "VALUE_GREATER_OR_EQUAL_ALLOWED_LOWEST_LIMIT"
    override val description = "The value must be less than the maximum allowed limit."
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
