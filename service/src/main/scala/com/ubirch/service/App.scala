package com.ubirch.service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery.UriSpec

class CatApp(appPort: Int, appAddress: String, appName: String, appDescription: String) {

  object Service extends RegisterableService[InstanceDetails] {
    override val path: String = "/services"
    override val zookeeperAddress: String = "localhost:2181"
    override val instanceDetails: InstanceDetails = new InstanceDetails(appDescription)
    override val uriSpec: UriSpec = new UriSpec(s"{scheme}://$appAddress:{port}")
    override val zookeeperCuratorClient: CuratorFramework = createClient
    override val serviceRegistry: DiscoverableRegistry = {
      new DiscoverableRegistry(appName, appAddress, appPort)
    }
  }

  object Http extends HttpAnimalCreator {
    override def port: Int = appPort
    override def address: String = appAddress
  }

  def registerShutdownHooks(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        CloseableUtils.closeQuietly(Service.serviceRegistry)
        CloseableUtils.closeQuietly(Service.zookeeperCuratorClient)
      }
    })
  }

  registerShutdownHooks()

}

object App {
  val cat = new CatApp(8081, "localhost", "cat", "This is a cat creator")

  import cat._

  def main(args: Array[String]): Unit = {
    Service.zookeeperCuratorClient.start()
    Service.serviceRegistry.start()
    Http.start()
  }
}

object App2 {
  val cat = new CatApp(8082, "localhost", "cat", "This is a cat creator")

  import cat._

  def main(args: Array[String]): Unit = {
    Service.zookeeperCuratorClient.start()
    Service.serviceRegistry.start()
    Http.start()
  }
}
