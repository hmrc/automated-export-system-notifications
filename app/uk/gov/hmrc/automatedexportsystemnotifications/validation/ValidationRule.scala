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
