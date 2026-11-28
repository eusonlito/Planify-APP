package com.lito.planify.updates

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.lito.planify.ui.components.PrimaryButton
import com.lito.planify.ui.components.OutlinedActionButton

@Composable
internal fun SystemUpdatePanel(model: SystemUpdateModel = viewModel()) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var updateRequested by rememberSaveable { mutableStateOf(false) }
    val downloading = model.percent != null
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.feedback("Si la actualización sigue pendiente, puedes abrir de nuevo el instalador.")
    }
    fun install() {
        val apk = model.verified ?: return
        try {
            installer.launch(installerIntent(context, apk))
        } catch (e: RuntimeException) {
            model.feedback("No se pudo abrir el instalador. Vuelve a comprobar la actualización.")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) install()
        else model.feedback("Para continuar, permite a Planify instalar aplicaciones desde esta fuente.")
    }

    LaunchedEffect(updateRequested, downloading, model.verified, lifecycle) {
        if (!updateRequested || downloading) return@LaunchedEffect
        if (model.verified == null) {
            updateRequested = false
            return@LaunchedEffect
        }
        lifecycle.withResumed {
            updateRequested = false
            if (context.packageManager.canRequestPackageInstalls()) install()
            else try {
                permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()))
            } catch (_: RuntimeException) {
                model.feedback("Abre Ajustes de Android y permite instalar aplicaciones desde esta fuente.")
            }
        }
    }
    LaunchedEffect(model) {
        if (model.verified == null && model.percent == null) model.check()
    }
    val onUpdate = {
        updateRequested = true
        if (model.verified == null) model.prepare()
    }
    val onCancel: () -> Unit = {
        updateRequested = false
        model.cancel()
    }
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(24.dp), color = colors.surface) {
        Column(Modifier.fillMaxWidth()) {
            VersionSheet(model)
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                UpdateStatus(model)
                when {
                    downloading -> OutlinedActionButton("Cancelar descarga", onCancel, Modifier.fillMaxWidth())
                    model.release != null -> PrimaryButton("Actualizar", onUpdate, Modifier.fillMaxWidth(), enabled = !updateRequested)
                }
                if (!downloading && !updateRequested) {
                    OutlinedActionButton("Buscar actualizaciones", model::check, Modifier.fillMaxWidth(), enabled = !model.checking)
                }
            }
        }
    }
}

@Composable
private fun VersionSheet(model: SystemUpdateModel) {
    val colors = MaterialTheme.colorScheme
    val paper = colors.primaryContainer
    val band = colors.primary
    val ink = colors.onPrimaryContainer
    val available = model.release?.version
    Column(Modifier.fillMaxWidth().background(paper)) {
        Row(Modifier.fillMaxWidth().background(band).padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.SystemUpdateAlt, null, Modifier.size(22.dp), tint = colors.onPrimary)
            Text("Planify", style = MaterialTheme.typography.titleMedium, color = colors.onPrimary, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (available == null) "Versión instalada" else "Nueva versión", style = MaterialTheme.typography.bodyMedium, color = ink)
                Text(available ?: model.installed, style = MaterialTheme.typography.displayLarge, color = ink, fontWeight = FontWeight.Bold)
                if (available != null) Text("Instalada · ${model.installed}", style = MaterialTheme.typography.bodyMedium, color = ink)
            }
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Spacer(Modifier.matchParentSize().rotate(-12f).background(band.copy(alpha = 0.14f), RoundedCornerShape(14.dp)))
                Icon(if (available == null) Icons.Outlined.Layers else Icons.Outlined.FileDownload, null,
                    Modifier.matchParentSize().background(band, RoundedCornerShape(14.dp)).padding(13.dp),
                    tint = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun UpdateStatus(model: SystemUpdateModel) {
    val percent = model.percent
    Text(model.message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleMedium)
    if (percent != null) {
        LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
        Text("$percent %", style = MaterialTheme.typography.bodyMedium)
    } else if (model.checking) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
    }
}
