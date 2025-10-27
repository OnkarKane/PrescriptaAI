package com.example.prescriptaai.fragments

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.prescriptaai.BuildConfig
import com.example.prescriptaai.databinding.FragmentScanBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Intent // <-- ADD THIS
import com.example.prescriptaai.activity.ConfirmPrescriptionActivity // <-- ADD THIS

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan prescriptions.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkPermissionAndStartCamera()
        binding.btnTakePicture.setOnClickListener {
            takePhoto()
        }
    }

    private fun checkPermissionAndStartCamera() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e("ScanFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        setLoading(true)

        val name = "PrescriptaAI-Image-${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        // --- THIS IS THE PART THAT WAS BUGGY ---
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // Set up image capture listener, which is triggered after photo has been taken
        // THE BUG FIX IS HERE: We now pass 'outputOptions' to the function
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        val imageUri = outputFileResults.savedUri
                        val inputStream = requireContext().contentResolver.openInputStream(imageUri!!)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        if (bitmap != null) {
                            sendImageToGemini(bitmap)
                        } else {
                            Toast.makeText(requireContext(), "Failed to process image.", Toast.LENGTH_SHORT).show()
                            setLoading(false)
                        }
                    } catch (e: Exception) {
                        Log.e("ScanFragment", "Failed to load bitmap: ${e.message}", e)
                        setLoading(false)
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("ScanFragment", "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(requireContext(), "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                }
            }
        )
    }

    private fun sendImageToGemini(image: Bitmap) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
        val prompt = """
        You are a highly specialized medical AI for structured prescription data extraction. Your ONLY task is to provide a single, **VALID JSON OBJECT**. **NO** preamble, **NO** explanation, **NO** markdown fences (```json or ```), **NO** extra text whatsoever.

        ### CORE DIRECTIVES (STRICTLY ADHERE TO THESE)
        1.  **IMAGE IS ABSOLUTE TRUTH:** Prioritize the visual information from the image over any general medical knowledge or common drug patterns. Do NOT invent data or correct the prescription.
        2.  **EXACT JSON:** Your response MUST be a single, syntactically perfect JSON object according to the `FINAL JSON STRUCTURE`.
        3.  **NULL FOR MISSING:** If a field cannot be reliably extracted or inferred from the image, set its value to `null`.
        4.  **OCR ACCURACY:** Pay *extreme* attention to handwritten characters, especially similar-looking ones (e.g., 'n' vs. 'v', 'l' vs. 't', 'a' vs. 'u', '0' vs 'O'). Ensure precise drug names and numbers.
        5.  **EXPAND ABBREVIATIONS:** Expand all common medical shorthand (e.g., "OD" -> "Once a day", "BD" -> "Twice a day").

        ### MEDICATION BLOCK HANDLING (CRITICAL)
        6.  Each distinct medication entry (e.g., "Tab. Augmentin", "Adv: Hexigel") constitutes ONE medication object. Any instructions or frequency/duration lines immediately below it belong to THAT SAME medication.
        7.  For each medication:
            -   `name`: Extract the full drug name and strength. **REMOVE prefixes like 'Tab.' (Tablet), 'Cap.' (Capsule), 'Adv:' from the final name.** (e.g., "Tab. Augmentin 625mg" becomes "Augmentin 625mg").
            -   `dosage`: The dose amount.
                -   **If the medication name includes 'Tab.' or 'Cap.' and no other numerical dosage is present, INFER the dosage as '1 tablet' or '1 capsule' respectively.**
                -   Specific instructions like "Massage" or "Apply externally" are considered dosage details.
            -   `frequency`: **STRICTLY MAP numeric patterns (e.g., `1-0-1`) to natural language.**
                -   "1-0-1" or "1--0--1" -> "Twice a day (Morning and Night)"
                -   "1-1-0" or "1--1--0" -> "Twice a day (Morning and Afternoon)"
                -   "0-1-1" or "0--1--1" -> "Twice a day (Afternoon and Night)"
                -   "1-1-1" or "1--1--1" -> "Three times a day (Morning, Afternoon, and Night)"
                -   "1-0-0" or "1--0--0" -> "Once a day (Morning)"
                -   "0-1-0" or "0--1--0" -> "Once a day (Afternoon)"
                -   "0-0-1" or "0--0--1" -> "Once a day (Night)"
                -   If no pattern is found, set to `null`.
            -   `duration`: The treatment length (e.g., "5 days", "1 week").

        ### DIAGNOSIS HANDLING
        8.  `diagnosis`: If explicitly written, extract it. If not, infer a "Potential: " diagnosis based on medications. If inference is too general, use "Potential: General symptomatic treatment".

        ### EXAMPLES (CRITICAL - Refer to these for expected output)
        **Example 1: Complex Medication Block (Hexigel)**
        Image Text:
        Adv: Hexigel gum paint
        Massage
        1-0-1 x 1 week
        JSON Output for this medication:
        {
          "name": "Hexigel gum paint",
          "dosage": "Massage",
          "frequency": "Twice a day (Morning and Night)",
          "duration": "1 week"
        }

        **Example 2: Dosage Inference (Augmentin)**
        Image Text:
        Tab. Augmentin 625mg
        1-0-1 x 5 days
        JSON Output for this medication:
        {
          "name": "Augmentin 625mg",
          "dosage": "1 tablet",
          "frequency": "Twice a day (Morning and Night)",
          "duration": "5 days"
        }

        **Example 3: Specific Frequency (Pan-D)**
        Image Text:
        Tab. Pan D 40mg
        1-0-0 x 5days
        JSON Output for this medication:
        {
          "name": "Pan D 40mg",
          "dosage": "1 tablet",
          "frequency": "Once a day (Morning)",
          "duration": "5 days"
        }

        ### FINAL JSON STRUCTURE (STRICTLY ADHERE TO THIS)
        {
          "patientName": "string_or_null",
          "patientPhoneNumber": "string_or_null",
          "age": "string_or_null",
          "diagnosis": "string_or_null",
          "medications": [
            {
              "name": "string",
              "dosage": "string_or_null",
              "frequency": "string_or_null",
              "duration": "string_or_null"
            }
          ],
          "prescriptionDate": "string_or_null"
        }
        """.trimIndent()

        lifecycleScope.launch {
            try {
                val inputContent = content {
                    image(image)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text

                if (responseText != null) {
                    // --- THIS IS THE NEW CODE ---
                    Log.d("GeminiResponse", responseText)
                    val intent = Intent(requireActivity(), ConfirmPrescriptionActivity::class.java).apply {
                        putExtra("GEMINI_RESPONSE", responseText)
                    }
                    startActivity(intent)
                    // --- END OF NEW CODE ---
                } else {
                    Toast.makeText(requireContext(), "Gemini returned an empty response.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("GeminiError", "Error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnTakePicture.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}