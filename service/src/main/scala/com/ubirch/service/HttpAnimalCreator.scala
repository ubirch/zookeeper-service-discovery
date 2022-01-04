package com.ubirch.service

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn
import scala.util.Random

trait HttpAnimalCreator {

  def port: Int
  def address: String

  def start(): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "animal-creator-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val route =
      path("create") {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Cat :: " + Random.nextInt().abs))
        }
      }

    val bindingFuture = Http().newServerAt(address, port).bind(route)

    println(s"Server now online. Please navigate to http://$address:$port/create\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

