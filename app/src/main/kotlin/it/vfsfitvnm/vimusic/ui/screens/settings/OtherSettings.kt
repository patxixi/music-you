package it.vfsfitvnm.vimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.service.PlayerMediaBrowserService
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid12
import it.vfsfitvnm.vimusic.utils.isAtLeastAndroid6
import it.vfsfitvnm.vimusic.utils.isIgnoringBatteryOptimizations
import it.vfsfitvnm.vimusic.utils.isInvincibilityEnabledKey
import it.vfsfitvnm.vimusic.utils.pauseSearchHistoryKey
import it.vfsfitvnm.vimusic.utils.rememberPreference
import it.vfsfitvnm.vimusic.utils.toast
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.android_auto),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.android_auto),
            text = stringResource(id = R.string.android_auto_description),
            isChecked = isAndroidAutoEnabled,
            onCheckedChange = { isAndroidAutoEnabled = it }
        )

        SettingsInformation(text = stringResource(id = R.string.android_auto_information))

        Spacer(modifier = Modifier.height(Dimensions.spacer))

        Text(
            text = stringResource(id = R.string.search_history),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.pause_search_history),
            text = stringResource(id = R.string.pause_search_history_description),
            isChecked = pauseSearchHistory,
            onCheckedChange = { pauseSearchHistory = it }
        )

        SettingsEntry(
            title = stringResource(id = R.string.clear_search_history),
            text = if (queriesCount > 0) {
                stringResource(id = R.string.delete_search_queries, queriesCount)
            } else {
                stringResource(id = R.string.history_is_empty)
            },
            onClick = { query(Database::clearQueries) },
            isEnabled = queriesCount > 0
        )

        Spacer(modifier = Modifier.height(Dimensions.spacer))

        Text(
            text = stringResource(id = R.string.service_lifetime),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium
        )

        SettingsEntry(
            title = stringResource(id = R.string.ignore_battery_optimizations),
            text = if (isIgnoringBatteryOptimizations) {
                stringResource(id = R.string.already_unrestricted)
            } else {
                stringResource(id = R.string.disable_background_restrictions)
            },
            onClick = {
                if (!isAtLeastAndroid6) return@SettingsEntry

                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.toast("Couldn't find battery optimization settings, please whitelist ViMusic manually")
                    }
                }
            },
            isEnabled = !isIgnoringBatteryOptimizations
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.service_lifetime),
            text = stringResource(id = R.string.service_lifetime_description),
            isChecked = isInvincibilityEnabled,
            onCheckedChange = { isInvincibilityEnabled = it }
        )

        SettingsInformation(
            text = stringResource(id = R.string.service_lifetime_information) +
                    if (isAtLeastAndroid12) "\n" + stringResource(id = R.string.service_lifetime_information_plus) else ""
        )
    }
}
