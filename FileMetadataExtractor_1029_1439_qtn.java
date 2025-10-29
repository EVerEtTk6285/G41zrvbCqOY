// 代码生成时间: 2025-10-29 14:39:20
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.buffer.Buffer;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileMetadataExtractor extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) {
        // Register a HTTP endpoint to handle file metadata extraction requests
        vertx.createHttpServer()
            .requestHandler(request -> {
                if ("POST".equals(request.method().name())) {
                    // Extract file path from request body
                    String filePath = request.getFormData().get("file");
                    if (filePath == null || filePath.isEmpty()) {
                        request.response().setStatusCode(400).end("File path missing");
                        return;
                    }

                    // Extract file metadata
                    extractFileMetadata(filePath)
                        .onComplete(ar -> {
                            if (ar.succeeded()) {
                                JsonObject metadata = (JsonObject) ar.result();
                                request.response().setStatusCode(200).putHeader("content-type", "application/json").end(metadata.encodePrettily());
                            } else {
                                request.response().setStatusCode(500).end("Failed to extract metadata");
                            }
                        });
                } else {
                    request.response().setStatusCode(405).end("Method Not Allowed");
                }
            })
            .listen(config().getInteger("http.port", 8080), result -> {
                if (result.succeeded()) {
                    startFuture.complete();
                } else {
                    startFuture.fail(result.cause());
                }
            });
    }

    /**
     * Extracts metadata from a file and returns it as a JSON object.
     * 
     * @param filePath The path to the file to extract metadata from.
     * @return A Future with the file metadata as a JSON object.
     */
    private Future<JsonObject> extractFileMetadata(String filePath) {
        Future<JsonObject> future = Future.future();
        // Read file using Vert.x file system
        vertx.fileSystem().open(filePath, new OpenOptions().setRead(true), res -> {
            if (res.succeeded()) {
                FileChannel fileChannel = res.result();
                // Use Java NIO to get file attributes
                try {
                    BasicFileAttributes attrs = Files.readAttributes(Paths.get(filePath), BasicFileAttributes.class);
                    JsonObject metadata = new JsonObject();
                    metadata.put("name", attrs.fileName());
                    metadata.put("size", attrs.size());
                    metadata.put("created", attrs.creationTime().toMillis());
                    metadata.put("lastModified", attrs.lastModifiedTime().toMillis());
                    metadata.put("lastAccessed", attrs.lastAccessTime().toMillis());
                    // Close the file channel
                    fileChannel.close();
                    future.complete(metadata);
                } catch (IOException e) {
                    future.fail(e);
                }
            } else {
                future.fail(res.cause());
            }
        });
        return future;
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new FileMetadataExtractor(), res -> {
            if (res.succeeded()) {
                System.out.println("File metadata extractor started");
            } else {
                System.err.println("Failed to start file metadata extractor");
                res.cause().printStackTrace();
            }
        });
    }
}
