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

import play.api.Logging
import uk.gov.hmrc.automatedexportsystemnotifications.models.requests.*

import scala.util.Try
import scala.xml.{Elem, Node, NodeSeq}

object EisPayloadXmlParser extends Logging:

  def parse(xmlString: String): Either[String, IncomingPayload] =
    Try(scala.xml.XML.loadString(xmlString)).toEither.left.map(_.getMessage).flatMap(parse)

  def parse(xml: Elem): Either[String, IncomingPayload] =
    for
      header      <- Helpers.requiredNode(xml, "Header")
      body        <- Helpers.requiredNode(xml, "Body")
      messageType <- Helpers.requiredText(header, "messageType")

      payload <- messageType match
                   case "ACK"    => parseAck(xml, body).map(IncomingPayload.Ack.apply)
                   case "CD906C" => parseIE906(xml, body).map(IncomingPayload.IE906.apply)
                   case "CD917C" => parseIE917(xml, body).map(IncomingPayload.IE917.apply)
                   case other    => Left(s"Unsupported messageType: $other")
    yield payload

  def parseAck(root: Node, body: Node): Either[String, AckBody] =
    for
      actionCode <- Helpers.requiredInt(body, "actionCode")
      mrn        <- Helpers.requiredText(body, "MRN")
      header     <- Helpers.requiredNode(root, "Header")
      eori       <- Helpers.requiredText(header, "messageRecipient")
    yield AckBody(ActionCode = actionCode, MRN = mrn, eori = eori)

  private def parseIE906(root: Node, body: Node): Either[String, IE906Body] =
    for
      mrn    <- Helpers.requiredText(body, "MRN")
      header <- Helpers.requiredNode(root, "Header")
      eori   <- Helpers.requiredText(header, "messageRecipient")
      errors <- parseFunctionalErrors(body.child.collect { case n: Node if n.label == "FunctionalError" => n })
    yield IE906Body(MRN = mrn, eori = eori, FunctionalError = errors)

  private def parseIE917(root: Node, body: Node): Either[String, IE917Body] =
    for
      mrn    <- Helpers.requiredText(body, "MRN")
      header <- Helpers.requiredNode(root, "Header")
      eori   <- Helpers.requiredText(header, "messageRecipient")
      errors <- parseXmlErrors(body.child.collect { case n: Node if n.label == "XMLError" => n })
    yield IE917Body(MRN = mrn, eori = eori, XmlError = errors)

  private def parseFunctionalErrors(nodes: Seq[Node]): Either[String, List[FunctionalError]] =
    Helpers.sequence(
      nodes.toList.map { n =>
        for
          pointer <- Helpers.requiredText(n, "errorPointer")
          code    <- Helpers.requiredInt(n, "errorCode")
          reason  <- Helpers.requiredText(n, "errorReason")
        yield FunctionalError(
          errorPointer = pointer,
          errorCode = code,
          errorReason = reason,
          originalAttribute = Helpers.optionalText(n, "originalAttributeValue")
        )
      }
    )

  private def parseXmlErrors(nodes: Seq[Node]): Either[String, List[XMLError]] =
    Helpers.sequence(
      nodes.toList.map { n =>
        for
          pointer <- Helpers.requiredText(n, "errorPointer")
          code    <- Helpers.requiredInt(n, "errorCode")
          text    <- Helpers.requiredText(n, "errorText")
        yield XMLError(
          errorPointer = pointer,
          errorCode = code,
          errorText = text,
          originalAttribute = Helpers.optionalText(n, "originalAttributeValue")
        )
      }
    )
