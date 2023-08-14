package ide.shota.colomney.PasskeyTest

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.util.Base64Utils
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import ide.shota.colomney.PasskeyTest.ui.theme.PasskeyTestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val createCredentialIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleCreateCredentialResult
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PasskeyTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),  // この行で余白全体を追加
                        verticalArrangement = Arrangement.Center, // ボタンを画面の中央に配置
                        horizontalAlignment = Alignment.CenterHorizontally // ボタンを水平方向の中央に配置
                    ) {
                        Greeting(
                            "Register with CredentialManager",
                            onClick = {
                                lifecycleScope.launch {
                                    viewModel.registerWithCredentialManager(context = this@MainActivity)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Greeting(
                            "Sign In with CredentialManager",
                            onClick = {
                                lifecycleScope.launch {
                                    viewModel.signInWithCredentialManager(context = this@MainActivity)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Greeting(
                            "Register with FIDO2 client",
                            onClick = {
                                lifecycleScope.launch {
                                    val pendingIntent = viewModel.register(applicationContext)
                                    createCredentialIntentLauncher.launch(
                                        IntentSenderRequest.Builder(pendingIntent).build()
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Greeting(
                            "Sign In with FIDO2 client",
                            onClick = {
                                lifecycleScope.launch {
                                    val pendingIntent = viewModel.signIn(applicationContext)
                                    createCredentialIntentLauncher.launch(
                                        IntentSenderRequest.Builder(pendingIntent).build()
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleCreateCredentialResult(activityResult: ActivityResult) {
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                println("Cancelled")

            bytes == null ->
                println("Credential error")

            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    println("Error: ${response.errorMessage}")
                } else {
                    println("Succeeded")
                    println("Base64 encoded ID: ${Base64Utils.encode(credential.rawId)}")
                    println("Type: ${credential.type}")
                }
            }
        }
    }
}

@Composable
fun Greeting(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Button(onClick = onClick!!) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PasskeyTestTheme {
        Greeting("Android")
    }
}