/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.voa.htmlcheck.elements

import cats.data.Xor
import cats.data.Xor.{Left, Right}
import org.jsoup.nodes.Element
import uk.gov.voa.htmlcheck.{ElementOfWrongType, HtmlCheckError}
import uk.gov.voa.htmlcheck.elements.ElementAttribute.{IdAttribute, TypeAttribute}

import scala.language.implicitConversions

trait Button extends Input

object Button {

  implicit def elementWrapper(element: Element): HtmlCheckError Xor Button =
    if (element.tagName() != "button")
      Left(ElementOfWrongType("button", s"${element.tagName()}${TypeAttribute(element).map(t => s"-$t").getOrElse("")}", IdAttribute(element)))
    else
      TypeAttribute(element) match {
        case Right(TypeAttribute("submit")) => Right(ButtonSubmit(element))
        case Right(TypeAttribute("reset")) => Right(ButtonReset(element))
        case _ => Right(GenericButton(element))
      }

}

case class GenericButton(protected val element: Element) extends Button

case class ButtonSubmit(protected val element: Element) extends Button

object ButtonSubmit {

  implicit def elementWrapper(element: Element): HtmlCheckError Xor ButtonSubmit =
    if (element.tagName() != "button" || !TypeAttribute(element).exists(_.value == "submit"))
      Left(ElementOfWrongType("button-submit", s"${element.tagName()}${TypeAttribute(element).map(t => s"-$t").getOrElse("")}", IdAttribute(element)))
    else
      Right(ButtonSubmit(element))

}

case class ButtonReset(protected val element: Element) extends Button

object ButtonReset {

  implicit def elementWrapper(element: Element): HtmlCheckError Xor ButtonReset =
    if (element.tagName() != "button" || !TypeAttribute(element).exists(_.value == "reset"))
      Left(ElementOfWrongType("button-reset", s"${element.tagName()}${TypeAttribute(element).map(t => s"-$t").getOrElse("")}", IdAttribute(element)))
    else
      Right(ButtonReset(element))

}
