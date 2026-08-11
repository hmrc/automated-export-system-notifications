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
  def code: String
}

object UnifiedErrorCode {
  case object InvalidMrn extends UnifiedErrorCode { val code = "INVALID_MRN" }
  case object MissingField extends UnifiedErrorCode { val code = "MISSING_FIELD" }
  case object Unknown extends UnifiedErrorCode { val code = "UNKNOWN_ERROR" }
}

sealed trait ErrorSource
object ErrorSource {
  case object IE906 extends ErrorSource
  case object IE917 extends ErrorSource
}

object ErrorMapper {

  import UnifiedErrorCode._
  import ErrorSource._

  private val ie906Map: Map[Int, UnifiedErrorCode] = Map(
    12 -> InvalidMrn,
    34 -> MissingField
  )

  private val ie917Map: Map[Int, UnifiedErrorCode] = Map(
    12 -> MissingField,
    99 -> InvalidMrn
  )

  def toUnified(source: ErrorSource, rawCode: Int): UnifiedErrorCode =
    source match {
      case IE906 => ie906Map.getOrElse(rawCode, Unknown)
      case IE917 => ie917Map.getOrElse(rawCode, Unknown)
    }
}
