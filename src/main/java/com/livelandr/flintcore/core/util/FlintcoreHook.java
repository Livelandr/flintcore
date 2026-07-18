package com.livelandr.flintcore.core.util;

@FunctionalInterface
public interface FlintcoreHook {
    float process(HookContext context);
}
