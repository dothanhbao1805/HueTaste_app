package com.example.huetaste.feature.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.widget.SeekBar
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.dispose
import coil.load
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.darkNavigationBar
import com.base.common.extension.darkStatusBar
import com.base.common.extension.gone
import com.base.common.extension.invisible
import com.base.common.extension.visible
import com.example.huetaste.R
import com.example.huetaste.common.extension.startAppSettings
import com.example.huetaste.common.extension.startScanDishDetailActivity
import com.example.huetaste.databinding.ActivityScanBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.feature.scan.dialog.ScanMessageDialog
import com.example.huetaste.feature.scan.sheet.ScanTipsSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class ScanActivity : LsActivity<ActivityScanBinding>(ActivityScanBinding::inflate) {

    @Inject lateinit var imageProcessor: ScanImageProcessor

    private val viewModel: ScanViewModel by viewModels()

    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var isTorchOn = false
    private var isRestored = false
    private var isWaitingForSettings = false
    private val captureExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionDialog()
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::identifyGalleryImage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            isRestored = true
            isWaitingForSettings = it.getBoolean(KEY_WAITING_FOR_SETTINGS)
        }
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        // Light system-bar icons over the black camera screen.
        darkStatusBar()
        darkNavigationBar()

        binding.closeButton.clicks { handleBackPressed() }
        binding.tipsButton.clicks { ScanTipsSheet.show(supportFragmentManager) }
        binding.snapButton.clicks { takePhoto() }
        binding.galleryButton.clicks {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.flashButton.clicks { toggleTorch() }
        binding.zoomMinusButton.clicks { stepZoom(-ZOOM_STEP) }
        binding.zoomPlusButton.clicks { stepZoom(ZOOM_STEP) }
        binding.zoomSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) camera?.cameraControl?.setLinearZoom(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        observeDialogResults()
        observeViewModel()

        if (!isRestored) lifecycleScope.launch { imageProcessor.clearOldFiles() }
        when {
            hasCameraPermission() -> startCamera()
            // A restored permission dialog or system prompt is still waiting for an answer.
            !isRestored -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isWaitingForSettings) return
        isWaitingForSettings = false
        if (hasCameraPermission()) startCamera() else showPermissionDialog()
    }

    override fun handleBackPressed() {
        if (viewModel.uiState.value is ScanUiState.Identifying) {
            viewModel.cancel()
        } else {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ScanEvent.OpenDetail -> {
                                startScanDishDetailActivity(event.scanId)
                                finish()
                            }
                            is ScanEvent.Failed -> showErrorDialog(event.error)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ScanUiState) {
        val identifying = state is ScanUiState.Identifying
        if (state is ScanUiState.Identifying) {
            // Keep the frozen camera frame if one was captured; otherwise show the saved photo.
            if (binding.capturedPreview.drawable == null) state.imagePath?.let { binding.capturedPreview.load(File(it)) }
            binding.capturedPreview.visible()
            binding.scanLineContainer.visible()
            binding.scanLine.playAnimation()
            binding.hintText.setText(R.string.scan_identifying)
        } else {
            binding.scanLine.cancelAnimation()
            binding.scanLineContainer.invisible()
            binding.capturedPreview.gone()
            binding.capturedPreview.dispose()
            binding.capturedPreview.setImageDrawable(null)
            binding.hintText.setText(R.string.scan_hint)
        }
        if (identifying) binding.blockTouchView.visible() else binding.blockTouchView.gone()
        binding.bottomBar.alpha = if (identifying) DISABLED_ALPHA else 1f
        binding.snapButton.alpha = if (identifying) DISABLED_ALPHA else 1f
        binding.tipsButton.isEnabled = !identifying
    }

    private fun startCamera() {
        if (imageCapture != null) return
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            if (isDestroyed) return@addListener
            val provider = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            // Big enough for recognition once cropped, small enough to decode without memory spikes.
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                            ResolutionStrategy(Size(CAPTURE_WIDTH, CAPTURE_HEIGHT), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                        )
                        .build(),
                )
                .build()
            try {
                provider.unbindAll()
                // CameraX releases the camera with this Activity's lifecycle.
                val boundCamera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                camera = boundCamera
                imageCapture = capture
                if (boundCamera.cameraInfo.hasFlashUnit()) binding.flashButton.visible() else binding.flashButton.invisible()
                boundCamera.cameraInfo.zoomState.observe(this) { zoom ->
                    binding.zoomSeekBar.progress = (zoom.linearZoom * 100).toInt()
                }
            } catch (e: Exception) {
                showErrorDialog(AppError(ScanViewModel.CAPTURE_FAILED, e.message, e))
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        if (viewModel.uiState.value is ScanUiState.Identifying) return
        // Freeze what the user framed while the full-resolution capture completes.
        binding.capturedPreview.setImageBitmap(binding.cameraPreview.bitmap)
        viewModel.startPreparing()

        val viewWidth = binding.cameraPreview.width
        val viewHeight = binding.cameraPreview.height
        val frameRect = Rect(
            binding.scanOverlay.left - binding.cameraPreview.left,
            binding.scanOverlay.top - binding.cameraPreview.top,
            binding.scanOverlay.right - binding.cameraPreview.left,
            binding.scanOverlay.bottom - binding.cameraPreview.top,
        )
        capture.takePicture(captureExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val rotation = image.imageInfo.rotationDegrees
                val bitmap: Bitmap? = runCatching { image.toBitmap() }.getOrNull()
                image.close()
                lifecycleScope.launch {
                    val file = bitmap?.let {
                        runCatching { imageProcessor.cropCapture(it, rotation, viewWidth, viewHeight, frameRect) }.getOrNull()
                    }
                    if (file != null) viewModel.identify(file) else viewModel.reportLocalFailure()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                lifecycleScope.launch { viewModel.reportLocalFailure() }
            }
        })
    }

    private fun identifyGalleryImage(uri: Uri) {
        if (viewModel.uiState.value is ScanUiState.Identifying) return
        viewModel.startPreparing()
        lifecycleScope.launch {
            val file = runCatching { imageProcessor.fromGallery(uri) }.getOrNull()
            if (file != null) {
                binding.capturedPreview.load(file)
                viewModel.identify(file)
            } else {
                viewModel.reportLocalFailure()
            }
        }
    }

    private fun toggleTorch() {
        val cam = camera ?: return
        isTorchOn = !isTorchOn
        cam.cameraControl.enableTorch(isTorchOn)
        binding.flashButton.setImageResource(if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
    }

    private fun stepZoom(step: Float) {
        val cam = camera ?: return
        val current = cam.cameraInfo.zoomState.value?.linearZoom ?: 0f
        cam.cameraControl.setLinearZoom((current + step).coerceIn(0f, 1f))
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionDialog() {
        // Once denied permanently the system no longer prompts, so only Settings can grant it.
        val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        ScanMessageDialog.show(
            supportFragmentManager,
            if (permanentlyDenied) REQUEST_PERMISSION_SETTINGS else REQUEST_PERMISSION,
            title = getString(R.string.camera_permission_title),
            message = getString(if (permanentlyDenied) R.string.camera_permission_settings_message else R.string.camera_permission_rationale),
            confirm = getString(if (permanentlyDenied) R.string.camera_permission_open_settings else R.string.camera_permission_allow),
            cancel = getString(R.string.camera_permission_not_now),
        )
    }

    private fun showErrorDialog(error: AppError) {
        val message = when (error.code) {
            AppError.NOT_RECOGNIZED -> getString(R.string.scan_error_not_recognized)
            AppError.DAILY_LIMIT -> getString(R.string.scan_error_daily_limit)
            AppError.FILE_TOO_LARGE, AppError.INVALID_IMAGE -> getString(R.string.scan_error_image)
            AppError.NETWORK -> getString(R.string.scan_error_network)
            ScanViewModel.CAPTURE_FAILED -> getString(R.string.scan_error_capture)
            else -> error.message?.takeIf { it.isNotBlank() } ?: getString(R.string.scan_error_generic)
        }
        ScanMessageDialog.show(
            supportFragmentManager,
            REQUEST_ERROR,
            title = getString(R.string.scan_error_title),
            message = message,
            confirm = getString(R.string.scan_retake),
            cancel = getString(R.string.scan_cancel),
        )
    }

    private fun observeDialogResults() {
        supportFragmentManager.setFragmentResultListener(REQUEST_PERMISSION, this) { _, result ->
            if (result.getBoolean(ScanMessageDialog.RESULT_CONFIRMED)) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                finish()
            }
        }
        supportFragmentManager.setFragmentResultListener(REQUEST_PERMISSION_SETTINGS, this) { _, result ->
            if (result.getBoolean(ScanMessageDialog.RESULT_CONFIRMED)) {
                isWaitingForSettings = true
                startAppSettings()
            } else {
                finish()
            }
        }
        supportFragmentManager.setFragmentResultListener(REQUEST_ERROR, this) { _, result ->
            if (result.getBoolean(ScanMessageDialog.RESULT_CONFIRMED)) viewModel.resetToCamera() else finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_WAITING_FOR_SETTINGS, isWaitingForSettings)
    }

    override fun onDestroy() {
        captureExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val KEY_WAITING_FOR_SETTINGS = "waiting_for_settings"
        private const val REQUEST_PERMISSION = "scan_permission"
        private const val REQUEST_PERMISSION_SETTINGS = "scan_permission_settings"
        private const val REQUEST_ERROR = "scan_error"
        private const val ZOOM_STEP = 0.1f
        private const val DISABLED_ALPHA = 0.35f
        private const val CAPTURE_WIDTH = 1920
        private const val CAPTURE_HEIGHT = 1440

        fun newIntent(context: Context): Intent = Intent(context, ScanActivity::class.java)
    }
}
