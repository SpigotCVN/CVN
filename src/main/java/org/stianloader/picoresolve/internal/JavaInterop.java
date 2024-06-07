package org.stianloader.picoresolve.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

public final class JavaInterop {
    public static String codepointToString(int codepoint) {
        return new String(new int[] {codepoint}, 0, 1);
    }
}
