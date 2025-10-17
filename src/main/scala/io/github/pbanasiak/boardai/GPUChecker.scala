package io.github.pbanasiak.boardai

import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.api.ndarray.INDArray

object GPUChecker {
  def verifyGPUUsage(): Unit = {
    println("=== GPU Backend Verification ===")

    // 1. Check backend type
    val backend = Nd4j.getBackend
    val backendName = backend.getClass.getSimpleName
    println(s"ND4J Backend: $backendName")

    // 2. Check if CUDA backend is detected
    val isCuda = backendName.toLowerCase.contains("cuda")
    if (isCuda) {
      println("✅ CUDA backend detected!")

      // 3. Try to get basic GPU info (works in M1.1)
      try {
        // Test GPU memory allocation
        val testArray = Nd4j.create(1000, 1000)
        testArray.assign(1.0)
        val result = testArray.sum()
        println(s"GPU memory test successful: sum = ${result.getDouble(0L)}")

        // Check execution engine
        val executioner = Nd4j.getExecutioner
        println(s"Executioner: ${executioner.getClass.getSimpleName}")

      } catch {
        case e: Exception =>
          println(s"GPU test failed: ${e.getMessage}")
          println("This might indicate GPU is not properly working")
      }

    } else {
      println("❌ CPU backend detected - GPU not being used!")
      println("Backend class: " + backend.getClass.getName)
      println("This means either:")
      println("- CUDA dependencies are not properly loaded")
      println("- No compatible GPU found")
      println("- CUDA installation issues")
    }

    // 4. Print system info
    try {
      val props = System.getProperties
      println(s"Java version: ${props.getProperty("java.version")}")
      println(s"OS: ${props.getProperty("os.name")} ${props.getProperty("os.arch")}")
    } catch {
      case _: Exception => // ignore
    }
  }

  def benchmarkMatrixMultiplication(): Unit = {
    println("\n=== GPU Performance Benchmark ===")

    val size = 1000
    val iterations = 5

    try {
      // Create matrices
      val a = Nd4j.rand(size, size)
      val b = Nd4j.rand(size, size)

      println(s"Testing ${size}x${size} matrix multiplication...")

      // Warm up
      for (_ <- 1 to 2) a.mmul(b)

      // Benchmark
      val startTime = System.currentTimeMillis()
      for (_ <- 1 to iterations) {
        val result = a.mmul(b)
      }
      val endTime = System.currentTimeMillis()

      val totalTime = endTime - startTime
      val avgTime = totalTime.toDouble / iterations

      println(f"Average time per operation: ${avgTime}%.2f ms")
      println(f"Total time for $iterations iterations: ${totalTime} ms")

      // Performance indicators
      if (avgTime < 100) {
        println("✅ Excellent performance - likely GPU acceleration")
      } else if (avgTime < 500) {
        println("⚠️ Moderate performance - GPU might be working but not optimally")
      } else {
        println("❌ Poor performance - likely CPU backend")
      }

    } catch {
      case e: Exception =>
        println(s"Benchmark failed: ${e.getMessage}")
    }
  }

  def checkGPUMemory(): Unit = {
    println("\n=== GPU Memory Test ===")

    try {
      // Create progressively larger arrays to test GPU memory
      val sizes = Array(100, 500, 1000, 2000)

      for (size <- sizes) {
        val startMem = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()

        val array = Nd4j.create(size, size)
        array.assign(1.0)
        val sum = array.sum()

        val endMem = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()
        val memDiff = (endMem - startMem) / (1024 * 1024) // MB

        println(f"${size}x${size} array: sum=${sum.getDouble(0L)}, JVM memory change: ${memDiff}%.1f MB")
      }

      println("If GPU is working properly, JVM memory usage should be minimal")
      println("because matrices are stored in GPU memory, not JVM heap")

    } catch {
      case e: Exception =>
        println(s"Memory test failed: ${e.getMessage}")
    }
  }
}