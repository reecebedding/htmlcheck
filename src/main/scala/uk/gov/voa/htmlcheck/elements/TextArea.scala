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
import cats.data.Xor._
import org.jsoup.nodes.Element
import uk.gov.voa.htmlcheck.{ElementWithIdOfWrongType, HtmlCheckError}

import scala.language.implicitConversions

case class TextArea(elementId: Option[ElementId],
                    value: Option[ElementValue],
                    errors: Seq[ErrorElement] = Nil)
                   (protected val element: Element)
  extends HtmlElement
    with ElementProperties
    with ErrorElements

object TextArea extends ErrorElementsFinder {

  implicit def textAreaElementWrapper(element: Element): HtmlCheckError Xor TextArea =
    if (element.tagName() != "textarea")
      Left(ElementWithIdOfWrongType(ElementId(element), "textarea", element.tagName()))
    else
      Right(TextArea(
        elementId = ElementId(element),
        value = ElementValue(element),
        errors = findElementErrors(element)
      )(element))
}