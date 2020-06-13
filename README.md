# pictorga project

Pictorga allows to organize pictures and videos according to their creation date and remove duplicates. 

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

## Packaging and running the application

The application can be packaged using `./mvnw package`.
It produces the `pictorga-1.0.0-SNAPSHOT-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/pictorga-1.0.0-SNAPSHOT-runner.jar`.

## Creating a native executable

You can create a native executable using: `./mvnw package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/pictorga-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.

## ERROR

22:41:50 ERROR traceId=c0e19d124563a265, spanId=c0e19d124563a265, sampled=true [or.gi.le.se.OrganizerService] (pool-3-thread-1) Unable to process the file '/home/eric/pictorga/input/MariageSeverineCyril_Pro/23.bmp': java.lang.LinkageError: javax/imageio/metadata/IIOMetadataNode
	at com.github.jaiimageio.impl.plugins.bmp.BMPMetadata.getNativeTree(BMPMetadata.java:180)
	at com.github.jaiimageio.impl.plugins.bmp.BMPMetadata.getAsTree(BMPMetadata.java:170)
	at org.apache.tika.parser.image.ImageParser.loadMetadata(ImageParser.java:103)
	at org.apache.tika.parser.image.ImageParser.parse(ImageParser.java:190)
	at org.apache.tika.parser.CompositeParser.parse(CompositeParser.java:280)
	at org.apache.tika.parser.ocr.TesseractOCRParser.parse(TesseractOCRParser.java:281)
	at org.apache.tika.parser.CompositeParser.parse(CompositeParser.java:280)
	at org.apache.tika.parser.CompositeParser.parse(CompositeParser.java:280)
	at org.apache.tika.parser.AutoDetectParser.parse(AutoDetectParser.java:143)
	at org.apache.tika.parser.AutoDetectParser.parse(AutoDetectParser.java:159)
	at org.github.leleueri.services.OrganizerService.parseFile(OrganizerService.java:181)
	at org.github.leleueri.services.OrganizerService.lambda$startOrganize$0(OrganizerService.java:78)
	at io.smallrye.mutiny.subscription.Subscribers$CallbackBasedSubscriber.onItem(Subscribers.java:107)
	at io.smallrye.mutiny.subscription.MultiSubscriber.onNext(MultiSubscriber.java:61)
	at io.smallrye.mutiny.context.ContextPropagationMultiInterceptor$1.lambda$onNext$1(ContextPropagationMultiInterceptor.java:36)
	at io.smallrye.context.SmallRyeThreadContext.lambda$withContext$0(SmallRyeThreadContext.java:215)
	at io.smallrye.mutiny.context.ContextPropagationMultiInterceptor$1.onNext(ContextPropagationMultiInterceptor.java:36)
	at io.smallrye.mutiny.context.ContextPropagationMultiInterceptor$1.lambda$onNext$1(ContextPropagationMultiInterceptor.java:36)
	at io.smallrye.context.SmallRyeThreadContext.lambda$withContext$0(SmallRyeThreadContext.java:215)
	at io.smallrye.mutiny.context.ContextPropagationMultiInterceptor$1.onNext(ContextPropagationMultiInterceptor.java:36)
	at io.smallrye.mutiny.operators.AbstractMulti$1.onNext(AbstractMulti.java:90)
	at io.smallrye.mutiny.subscription.SerializedSubscriber.onItem(SerializedSubscriber.java:69)
	at io.smallrye.mutiny.operators.multi.MultiEmitOnOp$MultiEmitOnProcessor.run(MultiEmitOnOp.java:208)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
