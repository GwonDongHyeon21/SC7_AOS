package com.example.controlward.ui

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.controlward.PostRequest
import com.example.controlward.R
import com.example.controlward.Value
import com.example.controlward.getDataFromDB
import com.example.controlward.getFileFromUri
import com.example.controlward.postDataToDB
import com.example.controlward.uploadImageToImgur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AddDisasterScreen(navController: NavController) {
    val context = LocalContext.current
    val cameraPermission = android.Manifest.permission.CAMERA
    var disasterText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { _ ->

        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4 / 3f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                    .padding(10.dp)
                    .clickable {
                        imageOptions(context) {
                            when (it) {
                                "카메라" -> permissionLauncher.launch(cameraPermission)
                                "갤러리" -> galleryLauncher.launch("image/*")
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.baseline_add_photo_alternate_24),
                        contentDescription = "",
                        modifier = Modifier.fillMaxSize(0.3f),
                        tint = Color.LightGray
                    )
                }
            }
            Spacer(modifier = Modifier.padding(bottom = 20.dp))

            BasicTextField(
                value = disasterText,
                onValueChange = { if (it.length <= 100) disasterText = it },
                modifier = Modifier
                    .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                    .padding(10.dp)
                    .fillMaxSize()
                    .weight(1f),
                decorationBox = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (disasterText.isEmpty())
                            Text(
                                text = "무슨 상황인가요?",
                                style = TextStyle(Color.Gray)
                            )
                    }
                    it()
                }
            )
            Spacer(modifier = Modifier.padding(bottom = 20.dp))

            Button(
                onClick = {
                    isLoading = false
                    val file = selectedImageUri?.let { getFileFromUri(context, it) }
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val imageUrl = file?.let { uploadImageToImgur(it) }
                            selectedImageUri = Uri.parse(imageUrl)

                            val postRequest = PostRequest(
                                Value.uid,
                                disasterText,
                                selectedImageUri.toString(),
                                listOf(Value.location.latitude, Value.location.longitude),
                            )
                            postDataToDB(postRequest)

                            disasterText = ""
                            selectedImageUri = null

                            getDataFromDB { disasters ->
                                Value.disasterAllList = disasters.toMutableList()
                                Value.disasterMap.clear()
                                disasters.forEach { Value.disasterMap[it.category]?.add(it) }
                            }

                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "오류가 발생했습니다. 잠시뒤 다시 시도해 주세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_add_24),
                    contentDescription = ""
                )
            }
        } else {
            LoadingScreen()
        }
    }
}

fun imageOptions(context: Context, option: (String) -> Unit) {
    val options = arrayOf("카메라", "갤러리")
    val builder = AlertDialog.Builder(context)
    builder.setTitle("이미지 가져오기")
        .setItems(options) { _, which ->
            when (which) {
                0 -> {
                    option("카메라")
                }

                1 -> {
                    option("갤러리")
                }
            }
        }
        .show()
}

@Preview(showBackground = true)
@Composable
fun PreviewAddDisaster() {
    AddDisasterScreen(rememberNavController())
}