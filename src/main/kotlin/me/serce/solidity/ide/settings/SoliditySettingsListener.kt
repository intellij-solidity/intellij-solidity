package me.serce.solidity.ide.settings

import com.intellij.util.messages.Topic

interface SoliditySettingsListener {
  fun settingsChanged()

  companion object {
    val TOPIC = Topic.create("Solidity Settings", SoliditySettingsListener::class.java)
  }
}
