package com.ubirch.service

import com.typesafe.scalalogging.LazyLogging
import monix.execution.atomic.AtomicBoolean
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery.UriSpec

import java.util.concurrent.CountDownLatch

class CatApp(appPort: Int, appAddress: String, appName: String, appDescription: String) extends LazyLogging {

  val isLeader: AtomicBoolean = AtomicBoolean(false)

  object Service extends RegisterableService[InstanceDetails] with LeaderLike {
    override val path: String = "/services"
    override val zookeeperAddress: String = "localhost:2181"
    override val instanceDetails: InstanceDetails = new InstanceDetails(appDescription)
    override val uriSpec: UriSpec = new UriSpec(s"{scheme}://$appAddress:{port}")
    override val zookeeperCuratorClient: CuratorFramework = createClient
    override val serviceRegistry: DiscoverableRegistry = {
      new DiscoverableRegistry(appName, appAddress, appPort)
    }

    val leadership: Leadership = new Leadership {
      override def takeLeadership(client: CuratorFramework): Unit = {
        try {
          logger.info("Taking leadership")
          isLeader.set(true)
          new CountDownLatch(1).await()

        } finally {
          logger.info("Relinquishing leadership")
          isLeader.set(false)
        }
      }
    }
  }

  object Http extends HttpCatCreator {
    override def createYellowCat: Boolean = isLeader.get()
    override def port: Int = appPort
    override def address: String = appAddress
  }

  def registerShutdownHooks(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        CloseableUtils.closeQuietly(Service.leadership)
        CloseableUtils.closeQuietly(Service.serviceRegistry)
        CloseableUtils.closeQuietly(Service.zookeeperCuratorClient)
      }
    })
  }

  registerShutdownHooks()

}

object App {

  def main(args: Array[String]): Unit = {

    val port = args.toList match {
      case List(p) => p.toInt
      case _ => throw new IllegalArgumentException("No port provided. Please provide a port")
    }

    val cat = new CatApp(port, "localhost", "cat", "This is a cat creator")

    import cat._

    Service.zookeeperCuratorClient.start()
    Service.serviceRegistry.start()
    Service.leadership.start()
    Http.start()
  }
}
