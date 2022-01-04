package com.ubirch.service

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery.UriSpec

object App {

  def appPort = 8081
  def appAddress = "localhost"
  def appName = "cat"
  def appDescription = "This is cat creator"

  object Service extends ServiceDiscoverable[InstanceDetails] {
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

  def main(args: Array[String]): Unit = {
    try {
      Service.zookeeperCuratorClient.start()
      Service.serviceRegistry.start()
      Http.start()
    } finally {
      CloseableUtils.closeQuietly(Service.serviceRegistry)
      CloseableUtils.closeQuietly(Service.zookeeperCuratorClient)
    }

  }

}
