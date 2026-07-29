/**
 * NeoForge-only overlay: the loader-specific implementations of the seams in the shared tree.
 * <p>
 * <strong>Every class in this package is excluded from the Fabric source set</strong> (see the {@code java.exclude}
 * block in {@code loader/fabric/build.gradle.kts}); the Fabric twins live in
 * {@code loader/fabric/src/main/java/de/mari_023/ae2wtlib/fabric}. Keeping the overlay inside {@code src/main/java}
 * rather than in a separate Gradle module means the NeoForge build stays exactly as upstream shipped it - it just
 * compiles a few more files - which is what keeps this fork rebaseable.
 */
package de.mari_023.ae2wtlib.neoforge;
