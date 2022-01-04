package com.ubirch.discovery

import com.ubirch.service.{ DiscoverableService, InstanceDetails }

import org.apache.curator.framework.CuratorFramework
import org.apache.curator.utils.CloseableUtils

import scala.io.StdIn
import scala.jdk.CollectionConverters._

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

    try {
      Discovery.zookeeperCuratorClient.start()
      Discovery.discoverableRegistry.start()
      Discovery.catAppCache.start()
      println(Discovery.catAppCache.getInstances.asScala)

      StdIn.readLine() // let it run until user presses return

    } finally {
      Discovery.catAppCache.close()
      CloseableUtils.closeQuietly(Discovery.discoverableRegistry)
      CloseableUtils.closeQuietly(Discovery.zookeeperCuratorClient)
    }

    ()

  }

}
