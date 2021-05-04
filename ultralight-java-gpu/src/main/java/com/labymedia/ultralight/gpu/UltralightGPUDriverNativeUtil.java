/*
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.labymedia.ultralight.gpu;

import com.labymedia.ultralight.gpu.os.Architecture;
import com.labymedia.ultralight.gpu.os.OperatingSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UltralightGPUDriverNativeUtil {

  private static UltralightGPUDriverNativeUtil instance;

  private UltralightGPUDriverNativeUtil() {

  }

  public static void extractNativeLibrary(Path nativesDir) throws IOException {
    OperatingSystem operatingSystem = OperatingSystem.get();
    Architecture architecture = Architecture.get();

    String nameWithArch = operatingSystem.mapLibraryName("ultralight-java-gpu-" + architecture.getBits());
    if (extractResource(nameWithArch, nativesDir.resolve(nameWithArch))) {
      return;
    }

    String nameWithoutArch = operatingSystem.mapLibraryName("ultralight-java-gpu");

    if (extractResource(nameWithoutArch, nativesDir.resolve(nameWithoutArch))) {
      return;
    }

    throw new RuntimeException("Failed to extract native library.");
  }

  /**
   * Extracts the given resource path to the given file. Parent directories are created as required.
   *
   * @param resourcePath The resource path to extract
   * @param targetFile   The path to the file to extract the resource to
   * @return {@code true} if the resource has been extracted, {@code false} otherwise
   * @throws IOException If an I/O error occurs during extraction
   */
  private static boolean extractResource(String resourcePath, Path targetFile) throws IOException {
    try (InputStream stream = UltralightGPUDriverNativeUtil.class.getClassLoader().getResourceAsStream("native-binaries/" + resourcePath)) {
      if (stream == null) {
        return false;
      }

      // Retrieve the target directory
      Path targetDir = targetFile.getParent();
      if (!Files.isDirectory(targetDir)) {
        // Create the target directory
        Files.createDirectories(targetDir);
      }

      // Copy the resource stream
      Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    return true;
  }

  /**
   * Tries to find the given library in the given directory.
   *
   * @param nativesDir      The directory to search the library in
   * @param libraryName     The name of the library to find
   * @param operatingSystem The operating system to find the library for
   * @param architecture    The architecture to find the library for
   * @return A path to the found library
   */
  private static Path determineLibraryPath(
      Path nativesDir,
      String libraryName,
      OperatingSystem operatingSystem,
      Architecture architecture
  ) {
    // First try to find the library from ${nativesDir}/${prefix}${name}-${bits}${suffix}
    Path pathWithArchitecture = nativesDir.resolve(
        operatingSystem.mapLibraryName(libraryName + "-" + architecture.getBits()));
    if (Files.isRegularFile(pathWithArchitecture)) {
      // Found it
      return pathWithArchitecture;
    }

    // Then try to find the library from ${nativesDir}/${prefix}${name}${suffix}
    Path pathWithoutArchitecture = nativesDir.resolve(operatingSystem.mapLibraryName(libraryName));
    if (Files.isRegularFile(pathWithoutArchitecture)) {
      return pathWithoutArchitecture;
    }

    throw new RuntimeException("Failed to find library " + libraryName);
  }

  public static void load(Path nativesDir) {
    OperatingSystem operatingSystem = OperatingSystem.get();
    Architecture architecture = Architecture.get();
    Path ultralightGpuLibrary =
        determineLibraryPath(nativesDir, "ultralight-java-gpu", operatingSystem, architecture);

    try {
      System.load(ultralightGpuLibrary.toAbsolutePath().toString());
    } catch (UnsatisfiedLinkError e) {
      throw new RuntimeException("Failed to load ultralight-java-gpu native library.", e);
    }
  }

  public static void extractAndLoadNativeLibraries(Path nativesDir) throws IOException {
    extractNativeLibrary(nativesDir);
    load(nativesDir);
  }

  public static UltralightGPUDriverNativeUtil getInstance() {

    if (UltralightGPUDriverNativeUtil.instance == null) {
      UltralightGPUDriverNativeUtil.instance = new UltralightGPUDriverNativeUtil();
    }
    return UltralightGPUDriverNativeUtil.instance;
  }

  public native long createOpenGLContext(long window, boolean msaa);

  public native long getDriverFromContext(long context);

  public native void beginSynchronize(long handle);

  public native void endSynchronize(long handle);

  public native boolean hasCommandsPending(long handle);

  public native void drawCommandList(long handle);

  public native void bindTexture(long handle, long textureId, long texture);

  public native void setActiveWindow(long handle, long window);
}
