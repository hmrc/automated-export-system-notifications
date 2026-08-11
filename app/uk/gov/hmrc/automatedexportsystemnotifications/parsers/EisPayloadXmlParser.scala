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

package uk.gov.hmrc.automatedexportsystemnotifications.parsers

import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.*

import scala.util.Try
import scala.xml.{Elem, Node, NodeSeq}

object EisPayloadXmlParser {

  def parse(xmlString: String): Either[String, IncomingPayload] =
    Try(scala.xml.XML.loadString(xmlString)).toEither.left.map(_.getMessage).flatMap(parse)

  def parse(xml: Elem): Either[String, IncomingPayload] =
    for {
      body        <- XmlNav.requiredNode(xml, "Body")
      messageCode <- Helpers.requiredText(body, "messageCode")
      payload     <- messageCode match {
                   case "CC507C" => parseAck(xml, body).map(IncomingPayload.Ack.apply)
                   case "CC906C" => parseIE906(xml, body).map(IncomingPayload.IE906.apply)
                   case "CD917C" => parseIE917(xml, body).map(IncomingPayload.IE917.apply)
                   case other    => Left(s"Unsupported messageCode: $other")
                 }
    } yield payload

  private def parseAck(root: Node, xml: Node): Either[String, AckBody] =
    for {
      actionCode <- Helpers.requiredInt(xml, "actionCode")
      mrn        <- Helpers.requiredText(xml, "MRN")
      header     <- XmlNav.requiredNode(root, "Header")
      eori       <- XmlNav.requiredText(header, "messageRecipient")
    } yield AckBody(ActionCode = actionCode, MRN = mrn, eori = eori)

  private def parseIE906(root: Node, xml: Node): Either[String, IE906Body] =
    for {
      mrn    <- Helpers.requiredText(xml, "MRN")
      header <- XmlNav.requiredNode(root, "Header")
      eori   <- XmlNav.requiredText(header, "messageRecipient")
      errors <- parseFunctionalErrors(xml \ "FunctionalError")
    } yield IE906Body(MRN = mrn, eori = eori, FunctionalError = errors)

  private def parseIE917(root: Node, xml: Node): Either[String, IE917Body] =
    for {
      mrn    <- Helpers.requiredText(xml, "MRN")
      header <- XmlNav.requiredNode(root, "Header")
      eori   <- XmlNav.requiredText(header, "messageRecipient")
      errors <- parseXmlErrors(xml \ "XmlError")
    } yield IE917Body(MRN = mrn, eori = eori, XmlError = errors)

  private def parseFunctionalErrors(nodes: NodeSeq): Either[String, List[FunctionalError]] = {
    val parsed = nodes.map { n =>
      for {
        pointer <- Helpers.requiredText(n, "errorPointer")
        code    <- Helpers.requiredInt(n, "errorCode")
        reason  <- Helpers.requiredText(n, "errorReason")
      } yield FunctionalError(
        errorPointer = pointer,
        errorCode = code,
        errorReason = reason,
        originalAttribute = Helpers.optionalText(n, "originalAttribute")
      )
    }.toList

    Helpers.sequence(parsed)
  }

  private def parseXmlErrors(nodes: NodeSeq): Either[String, List[XmlError]] = {
    val parsed = nodes.map { n =>
      for {
        pointer <- Helpers.requiredText(n, "errorPointer")
        code    <- Helpers.requiredInt(n, "errorCode")
        text    <- Helpers.requiredText(n, "errorText")
      } yield XmlError(
        errorPointer = pointer,
        errorCode = code,
        errorText = text,
        originalAttribute = Helpers.optionalText(n, "originalAttribute")
      )
    }.toList

    Helpers.sequence(parsed)
  }
}
