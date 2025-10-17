package io.github.pbanasiak.boardai.fiveinrow.nn

object FileHelper {
  def findLatestModelIteration(modelBasePath: String): Option[Int] = {
    import java.io.File
    import java.util.regex.Pattern

    try {
      // Extract directory and base filename from the path
      val file = new File(modelBasePath)
      val parentDir = Option(file.getParentFile).getOrElse(new File("."))
      val baseName = file.getName

      // Pattern to match model files: baseName_cnn_iter_NUMBER_policy.zip (we look for policy files specifically)
      val pattern = Pattern.compile(s"${Pattern.quote(baseName)}_cnn_iter_(\\d+)_policy\\.zip")

      println(s"DEBUG: Looking for pattern '${baseName}_cnn_iter_(\\d+)_policy.zip' in directory: ${parentDir.getAbsolutePath}")

      if (parentDir.exists() && parentDir.isDirectory) {
        val modelFiles = parentDir.listFiles()
        if (modelFiles != null) {
          println(s"DEBUG: Found ${modelFiles.length} files in directory")

          val matchingFiles = modelFiles
            .filter(_.isFile)
            .map(_.getName)
            .filter { filename =>
              val matches = pattern.matcher(filename).matches()
              println(s"DEBUG: File '$filename' matches pattern: $matches")
              matches
            }

          val iterations = matchingFiles.flatMap { filename =>
            val matcher = pattern.matcher(filename)
            if (matcher.matches()) {
              try {
                val iterNum = matcher.group(1).toInt
                println(s"DEBUG: Extracted iteration number: $iterNum from $filename")
                Some(iterNum)
              } catch {
                case _: NumberFormatException =>
                  println(s"DEBUG: Failed to parse iteration number from $filename")
                  None
              }
            } else None
          }

          if (iterations.nonEmpty) {
            val maxIter = iterations.max
            println(s"DEBUG: Found iterations: ${iterations.mkString(", ")}, max: $maxIter")
            Some(maxIter)
          } else {
            println("DEBUG: No iterations found")
            None
          }
        } else {
          println("DEBUG: Could not list files in directory")
          None
        }
      } else {
        println(s"DEBUG: Directory does not exist or is not a directory: ${parentDir.getAbsolutePath}")
        None
      }
    } catch {
      case e: Exception =>
        println(s"Error finding latest model iteration: ${e.getMessage}")
        e.printStackTrace()
        None
    }
  }
}