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

package uk.gov.hmrc.automatedexportsystemnotifications

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

/** Temporary test class that forces sbt to generate a `test-reports` directory so that Jenkins build stage can pass without errors like: "ERROR:
  * Specified HTML directory
  * '/home/jenkins/workspace/automated-export-system-team/automated-export-system-notifications/target/test-reports/html-report' does not exist."
  * Delete after the very first real test is created.
  */
class DeleteMeAfterFirstRealTestSpec extends AnyFreeSpecLike, Matchers:
  "dummyTest" in {
    1 shouldBe 1
  }
