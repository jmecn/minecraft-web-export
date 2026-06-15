package io.github.jmecn.minecraftwebexport.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

public final class ServerTasks {

    private ServerTasks() {
    }

    public static <T> T call(MinecraftServer server, Supplier<T> task) {
        if (server == null) {
            throw new IllegalStateException("integrated server required");
        }
        if (server.isSameThread()) {
            return task.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        server.execute(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("server task interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new CompletionException(cause);
        }
    }
}
