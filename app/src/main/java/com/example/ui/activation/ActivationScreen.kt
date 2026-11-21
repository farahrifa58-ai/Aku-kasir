package com.example.ui.activation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.firebase.LicenseActivationRepository
import com.example.data.firebase.LicenseActivationResult
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import kotlinx.coroutines.launch

/**
 * Opens Gmail directly (preferred) or falls back to the default email app chooser
 * pre-addressed to rendykurniawan0311@gmail.com.
 */
fun openAdminPurchaseEmail(
    context: Context,
    businessName: String,
    deviceId: String,
    fallbackCopiedMsg: String
) {
    val adminEmail = LicenseActivationRepository.ADMIN_SUPPORT_EMAIL
    val subject = "Pembelian Kode Aktivasi ADA KASIR"
    val body = buildString {
        appendLine("Halo Admin ADA KASIR,")
        appendLine()
        appendLine("Saya ingin membeli Kode Aktivasi 1 Perangkat untuk aplikasi ADA KASIR.")
        if (businessName.isNotBlank()) {
            appendLine("Nama Usaha: ${businessName.trim()}")
        }
        if (deviceId.isNotBlank()) {
            appendLine("ID Perangkat: $deviceId")
        }
        appendLine()
        append("Terima kasih.")
    }

    val mailtoUri = Uri.parse(
        "mailto:$adminEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )

    // 1. Prefer Gmail app directly
    val gmailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
        setPackage("com.google.android.gm")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    // 2. Fallback to any installed email client
    val genericMailIntent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(gmailIntent)
    } catch (_: Exception) {
        try {
            context.startActivity(Intent.createChooser(genericMailIntent, "Hubungi Admin via Email"))
        } catch (_: Exception) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Admin Email", adminEmail))
            Toast.makeText(context, fallbackCopiedMsg, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun ActivationScreen(
    deviceId: String,
    onActivate: suspend (businessName: String, activationCode: String) -> Result<LicenseActivationResult>,
    onActivationSuccess: (LicenseActivationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var businessName by rememberSaveable { mutableStateOf("") }
    var activationCode by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submitActivation() {
        if (isLoading) return
        val cleanBusiness = businessName.trim()
        val cleanCode = activationCode.trim().uppercase()
        if (cleanBusiness.isBlank()) {
            errorMessage = strings.activationEmptyBusinessError
            return
        }
        if (cleanCode.isBlank()) {
            errorMessage = strings.activationEmptyCodeError
            return
        }
        errorMessage = null
        isLoading = true
        scope.launch {
            val result = onActivate(cleanBusiness, cleanCode)
            isLoading = false
            result.onSuccess { activation ->
                onActivationSuccess(activation)
            }.onFailure { err ->
                errorMessage = err.message?.takeIf { it.isNotBlank() } ?: strings.loginErrorFallback
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PointOfSale,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${strings.appName} • ${strings.developerBy}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Hero Banner
            Image(
                painter = painterResource(id = R.drawable.img_pos_login_hero),
                contentDescription = strings.activationTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(
                text = strings.activationTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("activation_screen_title")
            )

            Text(
                text = strings.activationSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Activation Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Offline-First info banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BrandGreen.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.activationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Input 1: Nama Usaha
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = {
                            businessName = it.take(80)
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text(strings.activationBusinessNameLabel) },
                        placeholder = { Text(strings.activationBusinessNameHint) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = BrandBlue
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_business_name_input")
                    )

                    // Input 2: Kode Aktivasi (Auto Uppercase)
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = { raw ->
                            activationCode = raw.uppercase().filter { ch ->
                                ch in 'A'..'Z' || ch in '0'..'9' || ch == '-' || ch == '_'
                            }.take(64)
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text(strings.activationCodeLabel) },
                        placeholder = { Text(strings.activationCodeHint) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = BrandBlue
                            )
                        },
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitActivation() }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_code_input")
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_error_banner")
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Primary CTA: Aktifkan Sekarang
                    Button(
                        onClick = { submitActivation() },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("activate_now_button")
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .testTag("activation_loading_indicator"),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = strings.activatingProgress,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.activateNowButton,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary CTA: Beli Kode / Hubungi Admin via Gmail
                    OutlinedButton(
                        onClick = {
                            openAdminPurchaseEmail(
                                context = context,
                                businessName = businessName,
                                deviceId = deviceId,
                                fallbackCopiedMsg = strings.activationEmailCopiedMsg
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("contact_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Email,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = strings.contactAdminButton,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                            Text(
                                text = strings.contactAdminSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (deviceId.isNotBlank()) {
                        Text(
                            text = "${strings.activationDeviceIdLabel}: $deviceId",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_device_id_text")
                        )
                    }
                }
            }
        }
    }
}
