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
import uk.gov.voa.htmlcheck.Html._
import uk.gov.voa.htmlcheck._
import uk.gov.voa.htmlcheck.elements.ElementAttribute._

import scala.collection.JavaConversions._
import scala.language.implicitConversions

trait HtmlElement {
  protected def element: Element
}

trait ElementProperties {

  self: HtmlElement =>

  lazy val id: Option[IdAttribute] = IdAttribute(element)

  lazy val name: Option[NameAttribute] = NameAttribute(element)

  lazy val className: Option[ClassAttribute] = ClassAttribute(element)

  lazy val classNames: Set[ClassAttribute] = element.classNames().toSet[String].map(ClassAttribute.apply)

  lazy val value: Option[ValueAttribute] = ValueAttribute(element)

  lazy val text: Option[ElementText] = ElementText(element)

  def attribute(name: AttributeName): Option[GenericAttribute] = GenericAttribute(name, element)

  def nextSibling[T <: HtmlElement](implicit elementWrapper: Element => HtmlCheckError Xor T,
                                    manifest: Manifest[T]): HtmlCheckError Xor T =
    Xor.fromOption(Option(element.nextElementSibling), ElementSiblingNotFound(id))
      .flatMap(elementWrapper)

  override def toString = IdAttribute(element).map(_.toString)
    .orElse(Option(element.tagName))
    .getOrElse(getClass.getSimpleName)
}

trait ContainerElement {

  self: HtmlElement =>

  def firstChild[T <: HtmlElement](implicit elementWrapper: Element => HtmlCheckError Xor T,
                                   manifest: Manifest[T]): HtmlCheckError Xor T =
    Xor.fromOption(Option(element.children().first()), ElementOfTypeNotFound(getTagTypeFromManifest, Some("as first child")))
      .flatMap(elementWrapper)

  def onlyChild[T <: HtmlElement](implicit elementWrapper: Element => HtmlCheckError Xor T,
                                  manifest: Manifest[T]): HtmlCheckError Xor T =
    element.children().toSeq match {
      case elements if elements.isEmpty => Left(ElementOfTypeNotFound(getTagTypeFromManifest, Some("as the only child")))
      case elements if elements.size > 1 => Left(MoreThanOneElementFound(elements.size, getTagTypeFromManifest))
      case elements => elementWrapper(elements.head)
    }

  def findDescendantBy[T <: HtmlElement](id: IdAttribute)(implicit elementWrapper: Element => HtmlCheckError Xor T,
                                                          manifest: Manifest[T]): HtmlCheckError Xor T =
    Xor.fromOption(Option(element.getElementById(id.value)), ElementOfWrongType(getTagTypeFromManifest, element.tagName(), Some(id)))
      .flatMap(elementWrapper)

  def findChildrenOfType[T <: HtmlElement](implicit elementWrapper: Element => HtmlCheckError Xor T,
                                           manifest: Manifest[T]): HtmlCheckError Xor Seq[T] = {
    implicit val contextualAttribute: Option[ElementAttribute] = None
    val identityPredicate: Element => Boolean = _ => true

    findChildrenMatching(identityPredicate).convertThemTo[T]
  }

  def findFirstChildOfType[T <: HtmlElement](implicit elementWrapper: Element => HtmlCheckError Xor T,
                                             manifest: Manifest[T]): HtmlCheckError Xor T =
    findChildrenOfType map (_.head)

  def findOnlyChildOfType[T <: HtmlElement](implicit elementWrapper: Element => HtmlCheckError Xor T,
                                            manifest: Manifest[T]): HtmlCheckError Xor T =
    findChildrenOfType flatMap {
      case head :: Nil => Right(head)
      case children => Left(MoreThanOneElementFound(children.size, getTagTypeFromManifest))
    }

  def findChildOfTypeByIndex[T <: HtmlElement](index: Int)(implicit elementWrapper: Element => HtmlCheckError Xor T,
                                                           manifest: Manifest[T]): HtmlCheckError Xor T =
    findChildrenOfType flatMap {
      case children if children.size > index => Right(children(index))
      case children => Left(ElementOutOfBounds(getTagTypeFromManifest, children.size, index))
    }

  def findChildrenBy[AT <: ElementAttribute, T <: HtmlElement](attribute: AT)(implicit filteringPredicate: AT => Element => Boolean,
                                                                              elementWrapper: Element => HtmlCheckError Xor T,
                                                                              manifest: Manifest[T]): HtmlCheckError Xor Seq[T] = {
    implicit val contextualAttribute: Option[ElementAttribute] = Some(attribute)

    findChildrenMatching(filteringPredicate(attribute)).convertThemTo[T]
  }

  def findFirstChildBy[AT <: ElementAttribute, T <: HtmlElement](attribute: AT)
                                                                (implicit filteringPredicate: AT => Element => Boolean,
                                                                 elementWrapper: Element => HtmlCheckError Xor T,
                                                                 manifest: Manifest[T]): HtmlCheckError Xor T =
    findChildrenBy[AT, T](attribute).map(_.head)

  def findOnlyChildBy[AT <: ElementAttribute, T <: HtmlElement](attribute: AT)(implicit filteringPredicate: AT => Element => Boolean,
                                                                               elementWrapper: Element => HtmlCheckError Xor T,
                                                                               manifest: Manifest[T]): HtmlCheckError Xor T =
    findChildrenBy[AT, T](attribute).flatMap {
      case head :: Nil => Right(head)
      case children => Left(MoreThanOneElementFound(children.size, getTagTypeFromManifest, attribute))
    }

  private def findChildrenMatching(predicate: Element => Boolean) =
    element.children.iterator().toSeq
      .filter(predicate)

  private implicit class ElementsOps(elements: Seq[Element]) {

    def convertThemTo[T <: HtmlElement](implicit elementAttribute: Option[ElementAttribute],
                                        elementWrapper: Element => HtmlCheckError Xor T,
                                        manifest: Manifest[T]): HtmlCheckError Xor Seq[T] =
      elements match {
        case Nil => Left(NoElementsFound(getTagTypeFromManifest, elementAttribute))
        case elmnts => elmnts
          .map(elementWrapper)
          .filter(errorOrElement => errorOrElement.isRight) match {
          case Nil => Left(NoElementsFound(getTagTypeFromManifest, elementAttribute))
          case xorsWithoutErrors => Right(xorsWithoutErrors.foldLeft(Seq.empty[T])((foundChildren, item) => foundChildren :+ item.getOrError))
        }
      }
  }

  private def getTagTypeFromManifest(implicit manifest: Manifest[_]) =
    manifest.runtimeClass.getSimpleName.toLowerCase

}

object HtmlElement {

  implicit val idPredicate: IdAttribute => Element => Boolean =
    id => _.id() == id.value

  implicit val classPredicate: ClassAttribute => Element => Boolean =
    className => _.classNames().toSet.contains(className.value)
}

