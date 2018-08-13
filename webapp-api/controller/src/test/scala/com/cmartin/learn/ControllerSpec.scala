package com.cmartin.learn

import com.cmartin.learn.common.sayHello
import org.scalatest.{FlatSpec, Matchers}

class ControllerSpec extends FlatSpec with Matchers {

  "Dummy test" should "pass" in {
    sayHello() shouldEqual "hello from common"
  }
}
