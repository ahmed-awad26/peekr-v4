package com.peekr.ui.settings.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.data.remote.telegram.TelegramAuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramLoginScreen(
    navController: NavController,
    viewModel: TelegramLoginViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (authState) {
            is TelegramAuthState.Authorized -> navController.popBackStack()
            is TelegramAuthState.Error -> isLoading = false
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ربط تليجرام") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color(0xFF0088CC).copy(alpha = 0.15f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (authState is TelegramAuthState.WaitingPassword) Icons.Default.Lock else Icons.Default.Send,
                        contentDescription = null,
                        tint = Color(0xFF0088CC),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            when (authState) {
                is TelegramAuthState.Idle, TelegramAuthState.WaitingPhone -> {
                    Text("أدخل رقم تليفونك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("هيتبعتلك كود تحقق على تليجرام", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("رقم التليفون") },
                        placeholder = { Text("+201234567890") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.sendPhone(phoneNumber)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = phoneNumber.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text("إرسال الكود")
                    }
                }

                TelegramAuthState.WaitingCode -> {
                    Text("أدخل كود التحقق", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("اتبعتلك كود على تليجرام", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("الكود") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.sendCode(code)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = code.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text("تأكيد")
                    }
                }

                TelegramAuthState.WaitingPassword -> {
                    Text("أدخل كلمة المرور", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("حسابك محمي بخطوة تحقق ثانية", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.sendPassword(password)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = password.isNotEmpty() && !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        else Text("دخول")
                    }
                }

                is TelegramAuthState.Error -> {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = (authState as TelegramAuthState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    // لو الخطأ بسبب مفقود API Keys، عرض زرار مباشر
                    val errMsg = (authState as TelegramAuthState.Error).message
                    if (errMsg.contains("API") || errMsg.contains("Hash") || errMsg.contains("ID")) {
                        Button(
                            onClick = { navController.navigate("settings/apikeys") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0088CC)
                            )
                        ) {
                            Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("الذهاب لمفاتيح API")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.initialize() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("إعادة المحاولة") }
                    }
                }

                TelegramAuthState.Authorized -> {
                    CircularProgressIndicator()
                    Text("تم الدخول، جاري التوجيه...")
                }
            }
        }
    }
}
