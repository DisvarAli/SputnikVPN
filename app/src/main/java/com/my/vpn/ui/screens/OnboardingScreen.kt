package com.my.vpn.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.my.vpn.R
import com.my.vpn.ui.theme.TunnelAqua

@Composable
fun OnboardingScreen(
    step: Int,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenInspiration: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    0 -> WelcomeStep(
                        onOpenDeveloper = onOpenDeveloper,
                        onOpenInspiration = onOpenInspiration
                    )
                    1 -> PermissionsStep()
                    else -> AgreementStep()
                }
            }

            Button(
                onClick = if (step < 2) onNext else onComplete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (step < 2) {
                        stringResource(R.string.onboarding_next)
                    } else {
                        stringResource(R.string.onboarding_start)
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Text(
                text = stringResource(R.string.onboarding_step, step + 1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onOpenDeveloper: () -> Unit,
    onOpenInspiration: () -> Unit
) {
    Spacer(modifier = Modifier.height(32.dp))
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        modifier = Modifier.size(72.dp),
        tint = TunnelAqua
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = stringResource(R.string.onboarding_owner),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "@DisvarAli",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TunnelAqua,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onOpenDeveloper)
            .padding(12.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_inspiration),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "@igareck — vpn-configs-for-russia",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TunnelAqua,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onOpenInspiration)
            .padding(12.dp),
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(R.string.onboarding_inspiration_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PermissionsStep() {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_permissions_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.onboarding_permissions_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_vpn_title),
            description = stringResource(R.string.onboarding_perm_vpn_body)
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_inet_title),
            description = stringResource(R.string.onboarding_perm_inet_body)
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_notif_title),
            description = stringResource(R.string.onboarding_perm_notif_body)
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_bg_title),
            description = stringResource(R.string.onboarding_perm_bg_body)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AgreementStep() {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_agreement_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AgreementParagraph(stringResource(R.string.onboarding_agree_p1))
            AgreementParagraph(stringResource(R.string.onboarding_agree_p2))
            AgreementParagraph(stringResource(R.string.onboarding_agree_p3))
            AgreementParagraph(stringResource(R.string.onboarding_agree_p4))
            AgreementParagraph(stringResource(R.string.onboarding_agree_b1))
            AgreementParagraph(stringResource(R.string.onboarding_agree_b2))
            AgreementParagraph(stringResource(R.string.onboarding_agree_b3))
            AgreementParagraph(stringResource(R.string.onboarding_agree_p5))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun AgreementParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun PermissionCard(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
