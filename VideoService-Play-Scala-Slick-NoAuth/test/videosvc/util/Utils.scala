package videosvc.util

import java.io.File
import java.nio.file.StandardCopyOption
import java.util.UUID

object Utils {

  def copyToTmpFile(srcFilename: String): String = {
    val tmpDir = "videos.tmp"
    createDirIfNotExists(tmpDir)
    val tmpFilename = tmpDir + "/" + UUID.randomUUID().toString
    java.nio.file.Files.copy(new File(srcFilename).toPath,
      new File(tmpFilename).toPath,
      StandardCopyOption.REPLACE_EXISTING)
    tmpFilename
  }

  def createDirIfNotExists(dir: String): Unit = {
    val path: java.nio.file.Path = new File(dir).toPath
    if (!java.nio.file.Files.exists(path))
      java.nio.file.Files.createDirectories(path)
  }

  def removeFileIfExists(filename: String): Unit = {
    java.nio.file.Files.deleteIfExists(new File(filename).toPath)
  }
}
