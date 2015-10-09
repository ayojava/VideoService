package videosvc.util

import java.io.File
import java.nio.file.StandardCopyOption

object Utils {

  def copyToTmpFile(videoDataFile: String): String = {
    val tmpDir = "videos.tmp"
    createDirIfNotExists(tmpDir)
    val basename = videoDataFile.substring(videoDataFile.lastIndexOf('/')+1)
    val tmpFile = tmpDir + "/" + basename
    java.nio.file.Files.copy(new File(videoDataFile).toPath, new File(tmpFile).toPath, StandardCopyOption.REPLACE_EXISTING)
    tmpFile
  }

  def createDirIfNotExists(dir: String): Unit = {
    val path: java.nio.file.Path = new File(dir).toPath
    if (!java.nio.file.Files.exists(path))
      java.nio.file.Files.createDirectories(path)
  }
}
