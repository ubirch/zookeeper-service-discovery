package com.ubirch.discovery

import com.ubirch.service.{ DiscoverableService, InstanceDetails }

import com.typesafe.scalalogging.LazyLogging
import monix.execution.Scheduler.Implicits.global
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{ ChildData, CuratorCacheListener }
import org.apache.curator.utils.CloseableUtils

import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration._
import scala.io.{ Source, StdIn }
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.Try

object App extends LazyLogging {

  val catApp = "cat"

  object Discovery extends DiscoverableService[InstanceDetails] {
    override val path: String = "/services"
    override val zookeeperAddress: String = "localhost:2181"
    override val zookeeperCuratorClient: CuratorFramework = createClient
    override val discoverableRegistry: DiscoverableRegistry = new DiscoverableRegistry()
    val watcher = new Watch
  }

  def main(args: Array[String]): Unit = {

    val currentCatURLs = new AtomicReference[List[URL]](Nil)

    Discovery.zookeeperCuratorClient.start()
    Discovery.discoverableRegistry.start()
    Discovery.watcher.start()

    Discovery.watcher.cache
      .listenable()
      .addListener((`type`: CuratorCacheListener.Type, oldData: ChildData, newData: ChildData) => {
        `type` match {
          case CuratorCacheListener.Type.NODE_CREATED =>
            newData.getPath.split("/").filter(_.nonEmpty).toList match {
              case List(path, app, id) =>
                logger.info("service detected=" + path + "/" + app + "/" + id)
                val newUrls = Discovery.serviceDiscovery.queryForInstances(catApp).asScala.filter(_.isEnabled).map { si =>
                  val params = Map("scheme" -> "http".asInstanceOf[AnyRef], "port" -> si.getPort.asInstanceOf[AnyRef]).asJava
                  new URL(si.buildUriSpec(params))
                }.toList
                currentCatURLs.set(newUrls)
              case _ =>
            }
          case CuratorCacheListener.Type.NODE_DELETED =>
            oldData.getPath.split("/").filter(_.nonEmpty).toList match {
              case List(path, app, id) =>
                logger.warn("service gone=" + path + "/" + app + "/" + id)
                val newUrls = Discovery.serviceDiscovery.queryForInstances(catApp).asScala.filter(_.isEnabled).map { si =>
                  val params = Map("scheme" -> "http".asInstanceOf[AnyRef], "port" -> si.getPort.asInstanceOf[AnyRef]).asJava
                  new URL(si.buildUriSpec(params))
                }.toList
                currentCatURLs.set(newUrls)
              case _ =>
            }
          case CuratorCacheListener.Type.NODE_CHANGED =>
        }
      })

    logger.info("Cat app discovered=" + currentCatURLs.get().size)

    global.scheduleWithFixedDelay(5 seconds, 5 seconds) {
      currentCatURLs.get().foreach { x =>
        Try("Creating " + x.toString + " - " + Source.fromURL(x.toString + "/create").getLines().mkString(" ")).map(println)
      }
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
