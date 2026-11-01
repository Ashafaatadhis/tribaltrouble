package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the desktop class in the Android build, which has no {@code ObjectInputFilter}. It applies the same
 * allow-list in {@code resolveClass}, for the pattern forms the game uses: {@code pkg.**} (the package and its
 * sub-packages), {@code pkg.*} (the package only) and a final {@code !*}. Dynamic proxies are always rejected.
 */
public final class DeserializationFilter {
    private static final String DENY_ALL = "!*";
    private static final String TREE_SUFFIX = ".**";
    private static final String PACKAGE_SUFFIX = ".*";
    private static final int SINGLE_CHARACTER_TYPE_CODE = 1;

    private final @NonNull List<String> packages;
    private final @NonNull List<String> packageTrees;

    private DeserializationFilter(@NonNull List<String> packages, @NonNull List<String> packageTrees) {
        this.packages = packages;
        this.packageTrees = packageTrees;
    }

    public static @NonNull DeserializationFilter of(@NonNull String pattern) {
        List<String> packages = new ArrayList<>();
        List<String> packageTrees = new ArrayList<>();
        for (String part : pattern.split(";")) {
            if (part.equals(DENY_ALL)) {
                continue;
            }
            if (part.endsWith(TREE_SUFFIX)) {
                packageTrees.add(part.substring(0, part.length() - TREE_SUFFIX.length()));
            } else if (part.endsWith(PACKAGE_SUFFIX)) {
                packages.add(part.substring(0, part.length() - PACKAGE_SUFFIX.length()));
            } else {
                throw new IllegalArgumentException("Unsupported filter pattern: " + part);
            }
        }
        return new DeserializationFilter(List.copyOf(packages), List.copyOf(packageTrees));
    }

    public @NonNull ObjectInputStream open(@NonNull InputStream in) throws IOException {
        return new AllowListObjectInputStream(in, this);
    }

    private boolean allows(@NonNull String descriptorName) {
        String name = descriptorName;
        while (name.startsWith("[")) {
            name = name.substring(1);
        }
        if (name.length() == SINGLE_CHARACTER_TYPE_CODE) {
            return true;
        }
        if (name.startsWith("L") && name.endsWith(";")) {
            name = name.substring(1, name.length() - 1);
        }
        int lastDot = name.lastIndexOf('.');
        String packageName = lastDot < 0 ? "" : name.substring(0, lastDot);
        if (packages.contains(packageName)) {
            return true;
        }
        for (String tree : packageTrees) {
            if (packageName.equals(tree) || packageName.startsWith(tree + ".")) {
                return true;
            }
        }
        return false;
    }

    private static final class AllowListObjectInputStream extends ObjectInputStream {
        private final @NonNull DeserializationFilter filter;

        AllowListObjectInputStream(@NonNull InputStream in, @NonNull DeserializationFilter filter) throws IOException {
            super(in);
            this.filter = filter;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            if (!filter.allows(desc.getName())) {
                throw new InvalidClassException(desc.getName(), "Class is not allowed");
            }
            return super.resolveClass(desc);
        }

        @Override
        protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
            throw new InvalidClassException("proxy", "Proxy classes are not allowed");
        }
    }
}
