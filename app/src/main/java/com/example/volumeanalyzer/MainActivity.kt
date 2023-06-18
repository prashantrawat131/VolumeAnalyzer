package com.example.volumeanalyzer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.volumeanalyzer.ui.theme.VolumeAnalyzerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@androidx.camera.core.ExperimentalGetImage
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VolumeAnalyzerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainScreen("Mahesh")
                }
            }
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(name: String) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )
    )
    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    PermissionsRequired(
        multiplePermissionsState = permissionState,
        permissionsNotGrantedContent = { /*TODO*/ },
        permissionsNotAvailableContent = { /*TODO*/ }) {


        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var previewView: PreviewView = remember {
            PreviewView(context)
        }

        LaunchedEffect(previewView) {
            setUpCamera(
                context,
                previewView,
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA
            )
        }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) {

        }
        Text(text = "Hello $name!")
    }
}

@androidx.camera.core.ExperimentalGetImage
suspend fun setUpCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector
) {
    val preview = androidx.camera.core.Preview.Builder()
        .build()
        .apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
    val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .apply {
            setAnalyzer(
                ContextCompat.getMainExecutor(context),
                VolumeAnalyzer()
            )
        }
    val cameraProvider = getCameraProvider(context)
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview,
        imageAnalyzer
    )
}

@androidx.camera.core.ExperimentalGetImage
class VolumeAnalyzer : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        CO.log("Analyzing: ${image.width}x${image.height}")
        // Live detection and tracking
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()  // Optional
            .build()

        val objectDetector = ObjectDetection.getClient(options)
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                CO.log("Success: ${detectedObjects.size}")
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
                CO.log("Failure: ${e.message}")
            }
            .addOnCompleteListener {
                image.close()
            }

        image.close()
    }
}

suspend fun getCameraProvider(context: Context): ProcessCameraProvider {
    return suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(context).also { future ->
            future.addListener(Runnable {
                continuation.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }
}

@androidx.camera.core.ExperimentalGetImage
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    VolumeAnalyzerTheme {
        MainScreen("Android")
    }
}
