package com.larkspur.testing;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import redis.embedded.RedisServer;
import redis.embedded.core.ExecutableProvider;

public final class EmbeddedRedis {
  private static RedisServer server;
  private static int port;

  private EmbeddedRedis() {
  }

  public static synchronized void start() {
    if (server != null) {
      return;
    }
    port = freePort();
    server = server(port, false);
    try {
      server.start();
    } catch (Exception first) {
      port = freePort();
      server = server(port, true);
      try {
        server.start();
      } catch (Exception retry) {
        throw new IllegalStateException("Redis did not start", retry);
      }
    }
  }

  public static synchronized int port() {
    if (server == null) {
      throw new IllegalStateException("Redis has not been started");
    }
    return port;
  }

  public static synchronized void stop() {
    if (server != null) {
      try {
        server.stop();
      } catch (IOException ex) {
        throw new IllegalStateException("Redis did not stop", ex);
      }
      server = null;
    }
  }

  private static RedisServer server(int port, boolean unsignedMacBinary) {
    var builder = RedisServer.newRedisServer().port(port).setting("bind 127.0.0.1").setting("daemonize no");
    if (unsignedMacBinary) {
      builder.executableProvider(ExecutableProvider.newCachedUrlProvider(
          Path.of(System.getProperty("java.io.tmpdir"), "embedded-redis-hankcp"),
          ExecutableProvider.REDIS_7_2_MACOSX_14_SONOMA_HANKCP));
    }
    try {
      return builder.build();
    } catch (IOException ex) {
      throw new IllegalStateException("Could not build Redis", ex);
    }
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (java.io.IOException ex) {
      throw new IllegalStateException("No free port", ex);
    }
  }
}
