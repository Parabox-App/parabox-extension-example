package com.parabox.example.domain.service

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxKey
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxMetadata
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.connector.ParaboxService
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.PluginConnection
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.Profile
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.ReceiveMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendMessageDto
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.SendTargetType
import com.ojhdtapp.paraboxdevelopmentkit.messagedto.message_content.PlainText
import com.parabox.example.core.util.DataStoreKeys
import com.parabox.example.core.util.NotificationUtil
import com.parabox.example.core.util.dataStore
import com.parabox.example.domain.util.CustomKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ConnService : ParaboxService() {
    companion object {
        var connectionType = 0
    }

    private fun receiveTestMessage(msg: Message, metadata: ParaboxMetadata) {
        val content = (msg.obj as Bundle).getString("content") ?: "No content"
        val profile = Profile(
            name = "anonymous",
            avatar = "https://gravatar.loli.net/avatar/d41d8cd98f00b204e9800998ecf8427e?d=mp&v=1.5.1",
            id = 1L
        )
        receiveMessage(
            ReceiveMessageDto(
                contents = listOf(PlainText(text = content)),
                profile = profile,
                subjectProfile = profile,
                timestamp = System.currentTimeMillis(),
                messageId = null,
                pluginConnection = PluginConnection(
                    connectionType = connectionType,
                    sendTargetType = SendTargetType.USER,
                    id = 1L
                )
            ),
            onResult = {
                if (it is ParaboxResult.Success) {
                    sendCommandResponse(
                        isSuccess = true,
                        metadata = metadata,
                        extra = Bundle().apply {
                            putString(
                                "message",
                                "Message received at ${System.currentTimeMillis()}"
                            )
                        }
                    )
                } else {
                    sendCommandResponse(
                        isSuccess = false,
                        metadata = metadata,
                        errorCode = (it as ParaboxResult.Fail).errorCode
                    )
                }
            }
        )
    }

    override fun customHandleMessage(msg: Message, metadata: ParaboxMetadata) {
        when (msg.what) {
            CustomKey.COMMAND_RECEIVE_TEST_MESSAGE -> {
                receiveTestMessage(msg, metadata)
            }
        }
    }

    override fun onMainAppLaunch() {
        Log.d("parabox", "onMainAppLaunch")
        // Auto Login
        if (getServiceState() == ParaboxKey.STATE_STOP) {
            lifecycleScope.launch {
                val isAutoLoginEnabled =
                    dataStore.data.first()[DataStoreKeys.AUTO_LOGIN] ?: false
                if (isAutoLoginEnabled) {
                    onStartParabox()
                }
            }
        }
    }

    override suspend fun onRecallMessage(messageId: Long): Boolean {
        return true
    }

    override fun onRefreshMessage() {

    }

    override suspend fun onSendMessage(dto: SendMessageDto): Boolean {

        return true
    }

    override fun onStartParabox() {
        NotificationUtil.startForegroundService(this)
        lifecycleScope.launch {
            updateServiceState(ParaboxKey.STATE_LOADING, "Step A")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_LOADING, "Step B")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_PAUSE, "Step C")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_LOADING, "Step D")
            delay(1000)
            updateServiceState(ParaboxKey.STATE_RUNNING, "Step E")
        }

    }

    override fun onStateUpdate(state: Int, message: String?) {

    }

    override fun onStopParabox() {
        NotificationUtil.stopForegroundService(this)
        updateServiceState(ParaboxKey.STATE_STOP)
    }

    override fun onCreate() {
        connectionType = packageManager.getApplicationInfo(
            this@ConnService.packageName,
            PackageManager.GET_META_DATA
        ).metaData.getInt("connection_type")
        super.onCreate()
    }

}