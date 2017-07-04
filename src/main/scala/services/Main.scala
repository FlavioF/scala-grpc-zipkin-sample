package services

import com.github.kristofa.brave.grpc.{BraveGrpcClientInterceptor, BraveGrpcServerInterceptor}
import com.github.kristofa.brave.{Brave, Sampler}
import com.google.common.base.Strings
import com.google.common.collect.Iterators
import io.grpc.{ManagedChannelBuilder, ServerBuilder, ServerInterceptors}
import services.kv.KeyValueServiceGrpc
import services.user.{UserRequest, UserServiceGrpc}
import zipkin.reporter.urlconnection.URLConnectionSender
import zipkin.reporter.{AsyncReporter, ReporterMetrics}

import scala.concurrent.ExecutionContext

object Main extends App {
  var DOCKER_IP = System.getProperty("docker.ip")
  if (Strings.isNullOrEmpty(DOCKER_IP)) DOCKER_IP = "localhost"

  val kvPort = 15001
  val userPort = 15002

  val kvBrave = brave("kv");
  val kvSrv = ServerBuilder
    .forPort(kvPort)
    .addService(
      ServerInterceptors.intercept(
        KeyValueServiceGrpc.bindService(new KeyValueServiceImpl, ExecutionContext.global),
        BraveGrpcServerInterceptor.create(kvBrave)
      )
    )
    .build

  val kvChannel = ManagedChannelBuilder.forAddress("localhost", kvPort).usePlaintext(true).build()
  val kvAsyncStub = KeyValueServiceGrpc.stub(kvChannel)
    .withInterceptors(BraveGrpcClientInterceptor.create(brave("kvAsyncClient")))
  val kvStub = KeyValueServiceGrpc.blockingStub(kvChannel)
    .withInterceptors(BraveGrpcClientInterceptor.create(brave("kvClient")))

  val userBrave = brave("user");
  val userSrv = ServerBuilder
    .forPort(userPort)
    .addService(
      ServerInterceptors.intercept(
        UserServiceGrpc.bindService(new UserServiceImpl(kvStub), ExecutionContext.global),
        BraveGrpcServerInterceptor.create(userBrave)
      )
    )
//    .executor(
//      BraveExecutorService.wrap(
//        Executors.newCachedThreadPool(
//          new ThreadFactoryBuilder()
//            .setNameFormat("grpc-default-executor" + "-%d")
//            .build()),
//        userBrave))
    .build

  val userChannel = ManagedChannelBuilder.forAddress("localhost", userPort).usePlaintext(true).build()
  val userAsyncStub = UserServiceGrpc.stub(userChannel)
    .withInterceptors(BraveGrpcClientInterceptor.create(brave("userAsyncClient")))
  val userStub = UserServiceGrpc.blockingStub(userChannel)
    .withInterceptors(BraveGrpcClientInterceptor.create(brave("userClient")))


  kvSrv.start()
  userSrv.start()

  val users = Iterators.cycle("karen", "bob", "john")
  (1 to 100).foreach { idx =>
    println(userStub.getUser(UserRequest(users.next())))
  }

  println("requests completed")

  private def brave(serviceName: String) = new Brave.Builder(serviceName)
    .traceSampler(Sampler.ALWAYS_SAMPLE)
    .reporter(AsyncReporter
      .builder(URLConnectionSender.create(String.format("http://%s:9411/api/v1/spans", DOCKER_IP)))
      .metrics(ReporterMetrics.NOOP_METRICS)
      .build())
    .build
}
