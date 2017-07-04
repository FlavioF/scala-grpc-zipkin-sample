## scala-grpc-zipkin-sample

Proof of concept to send tracing data from gRPC scalapb services to ZipKin

## Testing it

Starting zipkin server
```
docker run --name=zipkin -d -p 9411:9411 openzipkin/zipkin
```

Executing the sample application
```
./sbt run
```

Take a look into tracing results: http://localhost:9411/

