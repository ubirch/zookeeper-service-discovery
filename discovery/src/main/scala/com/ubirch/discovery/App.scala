package com.ubirch.discovery

import com.ubirch.service.{ DiscoverableService, InstanceDetails }

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils

import scala.io.{ Source, StdIn }
import scala.jdk.CollectionConverters._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

object App {

  def catApp = "cat"

  object Discovery extends DiscoverableService[InstanceDetails] {
    override val path: String = "/services"
    override val zookeeperAddress: String = "localhost:2181"
    override val zookeeperCuratorClient: CuratorFramework = createClient
    override val discoverableRegistry: DiscoverableRegistry = new DiscoverableRegistry()
    val catAppCache = discoverableRegistry.serviceDiscovery.serviceCacheBuilder().name(catApp).build()
  }

  def main(args: Array[String]): Unit = {

    Discovery.zookeeperCuratorClient.start()
    Discovery.discoverableRegistry.start()

    global.scheduleWithFixedDelay(5 seconds, 2 seconds) {
      Discovery.serviceDiscovery.queryForInstances(catApp).asScala.headOption.filter(_.isEnabled).map { si =>
        val params = Map("scheme" -> "http".asInstanceOf[AnyRef], "port" -> si.getPort.asInstanceOf[AnyRef]).asJava
        val url = si.buildUriSpec(params)
        val _ = Try(Source.fromURL(url + "/create").getLines().mkString(" ")).map(println)
      }.getOrElse(println(catApp + " not available"))

    }

    println(s"App running.\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return

    ()

  }

  def registerShutdownHooks(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        CloseableUtils.closeQuietly(Discovery.discoverableRegistry)
        CloseableUtils.closeQuietly(Discovery.zookeeperCuratorClient)
      }
    })
  }

  registerShutdownHooks()

}
