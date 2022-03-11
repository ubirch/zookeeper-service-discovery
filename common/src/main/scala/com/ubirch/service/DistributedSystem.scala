package com.ubirch.service

import org.apache.curator.framework.recipes.cache.CuratorCache
import org.apache.curator.framework.recipes.leader.{ LeaderSelector, LeaderSelectorListenerAdapter }
import org.apache.curator.framework.{ CuratorFramework, CuratorFrameworkFactory }
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.utils.CloseableUtils
import org.apache.curator.x.discovery.details.JsonInstanceSerializer
import org.apache.curator.x.discovery.{ ServiceDiscovery, ServiceDiscoveryBuilder, ServiceInstance, UriSpec }

import java.io.Closeable
import scala.reflect.ClassTag

trait ZooKeeperCurator {
  def zookeeperAddress: String
  def zookeeperCuratorClient: CuratorFramework
  def baseSleepTimeMs = 1000
  def maxRetries = 3
  def createClient: CuratorFramework = CuratorFrameworkFactory.newClient(
    zookeeperAddress,
    new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries)
  )
}

trait Registerable[T] {
  curator: ZooKeeperCurator =>

  def path: String
  def instanceDetails: T
  def uriSpec: UriSpec
  def serviceRegistry: DiscoverableRegistry

  class DiscoverableRegistry(serviceName: String, address: String, port: Int)(implicit ct: ClassTag[T]) extends Closeable {

    val instance: ServiceInstance[T] =
      ServiceInstance.builder[T]
        .name(serviceName)
        .payload(instanceDetails)
        .address(address)
        .port(port)
        .uriSpec(uriSpec)
        .build()

    val serializer = new JsonInstanceSerializer(ct.runtimeClass.asInstanceOf[Class[T]])

    val serviceDiscovery: ServiceDiscovery[T] =
      ServiceDiscoveryBuilder.builder(ct.runtimeClass.asInstanceOf[Class[T]])
        .client(curator.zookeeperCuratorClient)
        .basePath(path)
        .serializer(serializer)
        .thisInstance(instance)
        .build()

    def start(): Unit = serviceDiscovery.start()

    override def close(): Unit = CloseableUtils.closeQuietly(serviceDiscovery)
  }
}

trait Discoverable[T] {
  curator: ZooKeeperCurator =>

  def path: String
  def discoverableRegistry: DiscoverableRegistry
  def serviceDiscovery: ServiceDiscovery[T] = discoverableRegistry.serviceDiscovery

  class DiscoverableRegistry(implicit ct: ClassTag[T]) extends Closeable {

    val serializer = new JsonInstanceSerializer(ct.runtimeClass.asInstanceOf[Class[T]])

    val serviceDiscovery: ServiceDiscovery[T] =
      ServiceDiscoveryBuilder.builder(ct.runtimeClass.asInstanceOf[Class[T]])
        .client(curator.zookeeperCuratorClient)
        .basePath(path)
        .serializer(serializer)
        .build()

    def start(): Unit = serviceDiscovery.start()

    override def close(): Unit = CloseableUtils.closeQuietly(serviceDiscovery)
  }

  class Watch() extends Closeable {
    val cache: CuratorCache = CuratorCache.builder(curator.zookeeperCuratorClient, path).build()
    def start(): Unit = cache.start()
    override def close(): Unit = cache.close()
  }

}

trait LeaderLike {
  curator: ZooKeeperCurator =>

  def path: String

  abstract class Leadership extends LeaderSelectorListenerAdapter with Closeable {

    val leaderSelector = new LeaderSelector(curator.zookeeperCuratorClient, path, this)

    def start(): Unit = {
      leaderSelector.start()
      leaderSelector.autoRequeue()
    }

    override def close(): Unit = CloseableUtils.closeQuietly(leaderSelector)
  }
}

trait RegisterableService[T] extends ZooKeeperCurator with Registerable[T]

trait DiscoverableService[T] extends ZooKeeperCurator with Discoverable[T]

