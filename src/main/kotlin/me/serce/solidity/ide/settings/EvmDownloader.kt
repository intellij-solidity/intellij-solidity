package me.serce.solidity.ide.settings

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.templates.github.ZipUtil
import com.intellij.util.download.DownloadableFileService
import java.io.File
import javax.swing.JComponent

object EvmDownloader {
  private const val evmUrl = "https://bintray.com/ethereum/maven/download_file?file_path=org%2Fethereum%2Fethereumj-core%2F1.7.1-RELEASE%2Fethereumj-core-1.7.1-RELEASE.zip"

  private const val localName = "evm.zip"

  fun download(anchor: JComponent): String {
    val s = DownloadableFileService.getInstance()
    val createDownloader = s.createDownloader(listOf(s.createFileDescription(evmUrl, localName)), "EthereumJ VM bundle")
    val first = ProjectManager.getInstance().openProjects.first()
    val chooseFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Select destination folder for EVM installation"),
      anchor, first, null)
      ?: return ""
    val downloaded = createDownloader.downloadWithProgress(chooseFile.path, first, anchor)?.first()?.first ?: return ""
    val zipArchive = File(downloaded.path.removeSuffix("!/"))
    ProgressManager.getInstance().runProcessWithProgressSynchronously({
      ZipUtil.unzip(ProgressManager.getInstance().progressIndicator, File(chooseFile.path), zipArchive, null, null, true)
    }, "Unzipping", false, first)

    zipArchive.delete()
    return File(chooseFile.path, "lib").absolutePath
  }

}
