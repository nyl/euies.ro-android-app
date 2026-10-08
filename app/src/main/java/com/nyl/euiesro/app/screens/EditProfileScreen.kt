package com.nyl.euiesro.app.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nyl.euiesro.ApiClient
import com.nyl.euiesro.MediaUrlResolver
import com.nyl.euiesro.common.CurrentUserProfile
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    userName: String,
    currentUserProfile: CurrentUserProfile,
    onBack: () -> Unit,
    onProfileSaved: (CurrentUserProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var editedProfile by remember(currentUserProfile) {
        mutableStateOf(currentUserProfile)
    }
    val resolvedAvatar = remember(editedProfile.avatarUrl) {
        MediaUrlResolver.resolve(editedProfile.avatarUrl)
    }

    var username by remember(editedProfile.username) {
        mutableStateOf(editedProfile.username.ifBlank { userName })
    }
    var displayName by remember(editedProfile.name, editedProfile.username) {
        mutableStateOf(editedProfile.name ?: editedProfile.username.ifBlank { userName })
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            isUploading = true
            errorText = null
            val updated = ApiClient.uploadCurrentUserAvatar(context, uri)
            if (updated.id > 0) {
                editedProfile = updated
                onProfileSaved(updated)
            } else {
                errorText = "Nu s-a putut încărca poza de profil."
            }
            isUploading = false
        }
    }

    Scaffold(
        topBar = {
            EuIesroTopAppBar(
                title = "Edit profile",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!resolvedAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = resolvedAvatar,
                        contentDescription = "User profile picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Anonymous user profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (editedProfile.avatarUrl.isBlank()) "Adaugă" else "Înlocuiește")
                }

                if (editedProfile.avatarUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isUploading = true
                                errorText = null
                                val updated = ApiClient.deleteCurrentUserAvatar()
                                if (updated.id >= 0) {
                                    editedProfile = updated
                                    onProfileSaved(updated)
                                } else {
                                    errorText = "Nu s-a putut șterge poza de profil."
                                }
                                isUploading = false
                            }
                        },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Șterge")
                    }
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = username.ifBlank { "User" },
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val savedProfile = editedProfile.copy(
                        username = username.trim(),
                        name = displayName.trim().ifBlank { null }
                    )
                    onProfileSaved(savedProfile)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                Text("Save profile")
            }
        }
    }
}
