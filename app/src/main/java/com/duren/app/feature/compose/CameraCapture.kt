package com.duren.app.feature.compose

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.duren.app.ui.theme.DurenSpacing
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The camera-first (BeReal-style) capture surface. Opens straight to a live preview;
 * one tap captures a photo to the app cache and hands back a `file://` Uri (which the
 * media pipeline downscales + Base64-encodes onto the ember — no Storage needed).
 *
 * Free, on-device, no upload until the ember is actually sent. If permission is denied
 * or there's no camera, the user can always [onSkip] to the text/gallery composer.
 */
@Composable
fun CameraCapture(
    onCaptured: (Uri) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreviewSurface(
                onCaptured = onCaptured,
                onSkip = onSkip
            )
        } else {
            // No-permission fallback — never a dead end.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DurenSpacing.space6),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera’s off",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(DurenSpacing.space2))
                Text(
                    text = "Allow the camera to share a moment as it happens — or skip and write instead.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB4B4BB)
                )
                Spacer(Modifier.height(DurenSpacing.space5))
                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera", color = Color(0xFF2DD4BF))
                }
                TextButton(onClick = onSkip) {
                    Text("Skip camera", color = Color(0xFF8A8A92))
                }
            }
        }

        // Close/skip is always reachable.
        IconButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(DurenSpacing.space2)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Skip camera", tint = Color.White)
        }
    }
}

@Composable
private fun CameraPreviewSurface(
    onCaptured: (Uri) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var capturing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    // Held so teardown can unbind without re-blocking on the provider future.
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // (Re)bind whenever the lens flips.
    LaunchedEffect(lensFacing) {
        val provider = ProcessCameraProvider.getInstance(context).awaitProvider()
        cameraProvider = provider
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { cameraProvider?.unbindAll() } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .align(Alignment.Center)
                .clip(MaterialTheme.shapes.large)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = DurenSpacing.space6, vertical = DurenSpacing.space6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer to balance the switch button on the right.
            Spacer(Modifier.size(48.dp))

            // Shutter.
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (capturing) 0.5f else 1f))
                    .border(4.dp, Color(0xFF2DD4BF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (capturing) return@IconButton
                        capturing = true
                        capturePhoto(
                            context = context,
                            imageCapture = imageCapture,
                            onResult = { uri ->
                                capturing = false
                                if (uri != null) onCaptured(uri)
                            }
                        )
                    },
                    modifier = Modifier.size(64.dp)
                ) {}
            }

            // Flip camera.
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Switch camera",
                    tint = Color.White
                )
            }
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(DurenSpacing.space2)
        ) {
            Text("Skip camera", color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

private fun capturePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    onResult: (Uri?) -> Unit
) {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val file = File(context.cacheDir, "ember_$name.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onResult(output.savedUri ?: Uri.fromFile(file))
            }

            override fun onError(exception: ImageCaptureException) {
                onResult(null)
            }
        }
    )
}

/** Awaits a ListenableFuture<ProcessCameraProvider> without pulling in coroutines-guava. */
private suspend fun com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>.awaitProvider(): ProcessCameraProvider =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addListener(
            {
                runCatching { get() }
                    .onSuccess { cont.resumeWith(Result.success(it)) }
                    .onFailure { cont.resumeWith(Result.failure(it)) }
            },
            java.util.concurrent.Executor { it.run() }
        )
    }
