package com.ubirch.discovery

import com.ubirch.discovery.Boot.Factory

object Discovery {

  val factory: Factory = new Factory(List(new Binder2)) {}

  def main(args: Array[String]): Unit = {
    println("hello service")
  }

}
