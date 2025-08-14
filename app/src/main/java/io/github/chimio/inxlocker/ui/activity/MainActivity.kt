package io.github.chimio.inxlocker.ui.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.hook.factory.prefs
import io.github.chimio.inxlocker.ui.activity.ui.theme.InxLockerTheme
import io.github.chimio.inxlocker.util.Broadcasts

data class InstallerApp(
    val resolveInfo: ResolveInfo,
    val label: String,
    val packageName: String,
    val icon: Drawable
)

class MainActivity : ComponentActivity() {

    private fun getApkInstallerApps(): List<InstallerApp> {
        return try {
            val dummyApkUri = "content://nya.apk".toUri()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dummyApkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resolveInfos = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL or PackageManager.MATCH_DEFAULT_ONLY
            )

            resolveInfos.filter { resolveInfo ->
                resolveInfo.activityInfo.packageName
                resolveInfo.activityInfo.exported
            }.map { resolveInfo ->
                InstallerApp(
                    resolveInfo = resolveInfo,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }.sortedBy { it.label }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveSelectedInstaller(packageName: String) {
        prefs("selected_installer_package").edit {
            putString("selected_installer_package", packageName)
        }
        sendBroadcast(Intent(Broadcasts.ACTION_PREFS_UPDATED))
    }

    private fun getSavedInstallerPackage(): String? {
        return try {
            prefs("selected_installer_package").getString("selected_installer_package")
        } catch (_: Exception) {
            null
        }
    }

    private fun clearSelectedInstaller() {
        prefs("selected_installer_package").edit {
            remove("selected_installer_package")

        }
        sendBroadcast(Intent(Broadcasts.ACTION_PREFS_UPDATED))
    }

    private fun setLauncherIconVisible(isVisible: Boolean) {
        val aliasComponent = ComponentName(this, "${packageName}.Home")
        packageManager.setComponentEnabledSetting(
            aliasComponent,
            if (isVisible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun saveHideIconState(hide: Boolean) {
        prefs().edit {
            putBoolean("hide_launcher_icon", hide)
        }
        setLauncherIconVisible(!hide)
        sendBroadcast(Intent(Broadcasts.ACTION_PREFS_UPDATED))

    }

    private fun getHideIconState(): Boolean {
        return try {
            prefs().getBoolean("hide_launcher_icon", false)
        } catch (_: Exception) {
            false
        }
    }

    private fun saveDebugLogEnabled(enabled: Boolean) {
        try {
            prefs("selected_installer_package").edit {
                putBoolean("enable_debug_log", enabled)
            }
            sendBroadcast(Intent(Broadcasts.ACTION_PREFS_UPDATED))
        } catch (_: Exception) {
        }
    }

    private fun getDebugLogEnabled(): Boolean {
        return try {
            prefs("selected_installer_package").getBoolean("enable_debug_log", true)
        } catch (_: Exception) {
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InxLockerTheme {
                MainScreen()
            }
        }
    }

    @Composable
    private fun MainScreen() {
        val context = this@MainActivity
        var showInstallerDialog by remember { mutableStateOf(false) }
        val installerList = remember { getApkInstallerApps() }
        var selectedPackage by remember {
            mutableStateOf(getSavedInstallerPackage())
        }
        var hideIcon by remember { mutableStateOf(getHideIconState()) }
        var debugLogEnabled by remember { mutableStateOf(getDebugLogEnabled()) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ModuleStatusCard()
                }

                item {
                    InstallerSelectorCard(
                        selectedPackage = selectedPackage,
                        onShowDialog = { showInstallerDialog = true }
                    )
                }

                item {
                    HideIconCard(
                        hideIcon = hideIcon,
                        onToggle = { newState ->
                            hideIcon = newState
                            saveHideIconState(newState)
                            Toast.makeText(
                                context,
                                if (newState) "应用图标将被隐藏" else "应用图标将显示在桌面",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                item {
                    DebugLogCard(
                        enabled = debugLogEnabled,
                        onToggle = { newState ->
                            debugLogEnabled = newState
                            saveDebugLogEnabled(newState)
                            Toast.makeText(
                                context,
                                if (newState) "日志已开启" else "日志已关闭",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                item {
                    InstructionCard()
                }
            }
        }

        if (showInstallerDialog) {
            InstallerSelectionDialog(
                installerList = installerList,
                selectedPackage = selectedPackage,
                onDismiss = { showInstallerDialog = false },
                onInstallerSelected = { packageName ->
                    selectedPackage = packageName
                    saveSelectedInstaller(packageName)
                    showInstallerDialog = false
                    Toast.makeText(context, "已保存安装器设置", Toast.LENGTH_SHORT).show()
                },
                onClearSelection = {
                    selectedPackage = null
                    clearSelectedInstaller()
                    showInstallerDialog = false
                    Toast.makeText(context, "已恢复默认设置", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    @Composable
    private fun ModuleStatusCard() {
        val isActive = YukiHookAPI.Status.isModuleActive

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Column {
                    Text(
                        text = if (isActive) "模块已激活" else "模块未激活",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = if (isActive) {
                            "InxLocker 模块正常工作中"
                        } else {
                            "请在 Xposed 管理器中激活模块"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun InstallerSelectorCard(
        selectedPackage: String?,
        onShowDialog: () -> Unit
    ) {
        val installerList = remember { getApkInstallerApps() }
        val selectedInstaller = installerList.find { it.packageName == selectedPackage }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "默认安装器设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "选择用于安装APK文件的默认应用程序",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowDialog() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedInstaller != null) {
                            val d = selectedInstaller.icon
                            val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 128
                            val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 128
                            Image(
                                bitmap = d.toBitmap(w, h).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedInstaller.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = selectedInstaller.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Build,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "系统默认",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "使用系统默认安装器",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "点击更改",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DebugLogCard(
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "启用日志",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (enabled) "日志输出已开启" else "日志输出已关闭",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }

    @Composable
    private fun HideIconCard(
        hideIcon: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hideIcon) Icons.Filled.Check else Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "隐藏应用图标",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hideIcon) "图标隐藏，Xposed管理器中找到" else "图标已显示在桌面",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = hideIcon,
                    onCheckedChange = onToggle
                )
            }
        }
    }

    @Composable
    private fun InstallerSelectionDialog(
        installerList: List<InstallerApp>,
        selectedPackage: String?,
        onDismiss: () -> Unit,
        onInstallerSelected: (String) -> Unit,
        onClearSelection: () -> Unit
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.7f),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择默认安装器",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "关闭"
                            )
                        }
                    }

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            InstallerItem(
                                icon = null,
                                label = "系统默认",
                                packageName = "使用系统默认安装器",
                                isSelected = selectedPackage == null,
                                onClick = onClearSelection
                            )
                        }

                        if (installerList.isNotEmpty()) {
                            itemsIndexed(installerList) { _, installer ->
                                InstallerItem(
                                    icon = installer.icon,
                                    label = installer.label,
                                    packageName = installer.packageName,
                                    isSelected = selectedPackage == installer.packageName,
                                    onClick = { onInstallerSelected(installer.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InstallerItem(
        icon: Drawable?,
        label: String,
        packageName: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    val d = icon
                    val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 128
                    val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 128
                    Image(
                        bitmap = d.toBitmap(w, h).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "已选择",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun InstructionCard() {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "使用说明",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = """确保模块已在 Xposed 框架中激活
选择默认安装器后，系统会优先使用该应用安装APK
可在Xposed管理器中找到应用设置""",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
                )
            }
        }
    }
}