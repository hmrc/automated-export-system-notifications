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
import cats.kernel.Semigroup
import uk.gov.hmrc.automatedexportsystemnotifications.errors.ValidationError

class ValidationRule[T, E <: ValidationError, U: Semigroup](f: T => ValidatedNel[E, U]):
  def validate(t: T): ValidatedNel[E, U] = f(t)

  def andThen(next: ValidationRule[T, E, U]): ValidationRule[T, E, U] =
    ValidationRule(t => validate(t).andThen(_ => next.validate(t)))

  def next(next: ValidationRule[T, E, U]): ValidationRule[T, E, U] =
    ValidationRule(t => validate(t).combine(next.validate(t)))
