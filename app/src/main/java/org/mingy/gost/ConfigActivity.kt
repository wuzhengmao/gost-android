package org.mingy.gost

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mingy.gost.ui.theme.GostTheme
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class ConfigActivity : ComponentActivity() {
    private val configEditText = MutableStateFlow("")
    private val isAutoStart = MutableStateFlow(false)
    private lateinit var configFile: File
    private lateinit var preferences: SharedPreferences

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gostConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.extras?.getParcelable(IntentExtraKey.GostConfig, GostConfig::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.extras?.getParcelable(IntentExtraKey.GostConfig)
        }
        if (gostConfig == null) {
            Log.e("gost", "Gost config is null")
            Toast.makeText(this, "Gost config is null", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        configFile = gostConfig.getFile(this)
        preferences = getSharedPreferences("data", MODE_PRIVATE)
        readConfig()
        readIsAutoStart()

        enableEdgeToEdge()
        setContent {
            GostTheme {
                Scaffold(topBar = {
                    TopAppBar(title = {
                        Text("${getString(R.string.gost_for_android)} - ${BuildConfig.VERSION_NAME}/${BuildConfig.GostVersion}")
                    })
                }) { contentPadding ->
                    // Screen content
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .verticalScroll(rememberScrollState())
                            .scrollable(orientation = Orientation.Vertical,
                                state = rememberScrollableState { delta -> 0f })
                    ) {
                        MainContent()
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MainContent() {
        val openDialog = remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { saveConfig();closeActivity() }) {
                    Text(stringResource(R.string.saveConfigButton))
                }
                Button(onClick = { closeActivity() }) {
                    Text(stringResource(R.string.dontSaveConfigButton))
                }
                Button(onClick = { openDialog.value = true }) {
                    Text(stringResource(R.string.rename))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.auto_start_switch))
                Switch(checked = isAutoStart.collectAsStateWithLifecycle(false).value,
                    onCheckedChange = { setAutoStart(it) })
            }
            TextField(
                configEditText.collectAsStateWithLifecycle("").value,
                onValueChange = { configEditText.value = it },
                textStyle = MaterialTheme.typography.bodyMedium.merge(fontFamily = FontFamily.Monospace)
            )
        }
        if (openDialog.value) {
            RenameDialog(configFile.name.removeSuffix(".launch")) { openDialog.value = false }
        }
    }

    @Composable
    fun RenameDialog(
        originName: String,
        onClose: () -> Unit,
    ) {
        var text by remember { mutableStateOf(originName) }
        AlertDialog(title = {
            Text(stringResource(R.string.rename))
        }, icon = {
            Icon(
                painterResource(id = R.drawable.ic_rename), contentDescription = "Rename Icon"
            )
        }, text = {
            TextField(text, onValueChange = { text = it })
        }, onDismissRequest = {
            onClose()
        }, confirmButton = {
            TextButton(onClick = {
                renameConfig("$text.launch")
                onClose()
            }) {
                Text(stringResource(R.string.confirm))
            }
        }, dismissButton = {
            TextButton(onClick = {
                onClose()
            }) {
                Text(stringResource(R.string.dismiss))
            }
        })
    }

    fun readConfig() {
        if (configFile.exists()) {
            val mReader = configFile.bufferedReader()
            val mRespBuff = StringBuffer()
            val buff = CharArray(1024)
            var ch = 0
            while (mReader.read(buff).also { ch = it } != -1) {
                mRespBuff.append(buff, 0, ch)
            }
            mReader.close()
            configEditText.value = mRespBuff.toString()
        } else {
            Log.e("gost", "Config file is not exist")
            Toast.makeText(this, "Config file is not exist", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveConfig() {
        configFile.writeText(configEditText.value)
    }

    fun renameConfig(newName: String) {
        val originAutoStart = isAutoStart.value
        setAutoStart(false)
        val newFile = File(configFile.parent, newName)
        configFile.renameTo(newFile)
        configFile = newFile
        setAutoStart(originAutoStart)
    }

    fun readIsAutoStart() {
        isAutoStart.value =
            preferences.getStringSet(PreferencesKey.AUTO_START_GOST_LIST, emptySet())?.contains(configFile.name)
                ?: false
    }

    fun setAutoStart(value: Boolean) {
        val editor = preferences.edit()
        val set = preferences.getStringSet(PreferencesKey.AUTO_START_GOST_LIST, emptySet())?.toMutableSet()
        if (value) {
            set?.add(configFile.name)
        } else {
            set?.remove(configFile.name)
        }
        editor.putStringSet(PreferencesKey.AUTO_START_GOST_LIST, set)
        editor.apply()
        isAutoStart.value = value
    }

    fun closeActivity() {
        setResult(RESULT_OK)
        finish()
    }
}