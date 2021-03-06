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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import uk.gov.voa.htmlcheck.elements.Label.ForAttribute
import uk.gov.voa.htmlcheck.{AttributeNotFound, HtmlCheckError}

class LabelSpec extends HtmlElementSpec[Label] {

  lazy val tagName = "label"

  lazy val elementWrapper: (Element) => HtmlCheckError Xor Label = Label.elementWrapper

  lazy val elementApply: (Element) => Label = Label.apply

  "forAttribute" should {

    "return ForAttribute if the 'for' is defined on the tag" in {
      val snippet =
        """
          |<label for="abc" />
          |""".stripMargin

      val element = Label(Jsoup.parse(snippet).body().children().first())

      element.forAttribute shouldBe Right(ForAttribute("abc"))
    }

    "return None for 'for' when the attribute is not defined on the tag" in {
      val snippet =
        """
          |<label />
          |""".stripMargin

      val element = Label(Jsoup.parse(snippet).body().children().first())

      element.forAttribute shouldBe Left(AttributeNotFound(AttributeName("for")))
    }

    "return None for 'for' when the attribute has no value" in {
      val snippet =
        """
          |<label for="" />
          |""".stripMargin

      val element = Label(Jsoup.parse(snippet).body().children().first())

      element.forAttribute shouldBe Left(AttributeNotFound(AttributeName("for")))
    }
  }

}
