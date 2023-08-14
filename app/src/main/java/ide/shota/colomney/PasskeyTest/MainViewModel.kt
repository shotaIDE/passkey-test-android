package ide.shota.colomney.PasskeyTest

import android.app.PendingIntent
import android.content.Context
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.util.Base64Utils
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.RSAAlgorithm
import kotlinx.coroutines.tasks.await

class MainViewModel : ViewModel() {
    private val domain = "passkeytest.page.link"
    private val challenge = "neKjg-lPlgvdOuFxDb9HCLeFD5726DmLkrZofdWsoWk"
    private val userName = "Test User"
    private val base64EncodedCredentialId = "go6b2cAEfjnWC4hZpRecmw"

    suspend fun register(context: Context): PendingIntent {
        val publicKeyCredentialCreationOptions =
            PublicKeyCredentialCreationOptions.Builder().apply {
                setRp(
                    PublicKeyCredentialRpEntity(domain, "Passkey Test", null)
                )
                setUser(
                    PublicKeyCredentialUserEntity(userName.toByteArray(), userName, "", userName)
                )
                setChallenge(challenge.toByteArray())
                setAuthenticatorSelection(AuthenticatorSelectionCriteria.Builder().apply {
                    setAttachment(Attachment.PLATFORM)
                }.build())
                setTimeoutSeconds(60.toDouble())
                setParameters(
                    mutableListOf(
                        PublicKeyCredentialParameters(
                            PublicKeyCredentialType.PUBLIC_KEY.toString(),
                            EC2Algorithm.ES256.algoValue
                        ),
                        PublicKeyCredentialParameters(
                            PublicKeyCredentialType.PUBLIC_KEY.toString(),
                            RSAAlgorithm.RS256.algoValue
                        )
                    )
                )
            }.build()

        val fidoClient = Fido.getFido2ApiClient(context)
        val task = fidoClient.getRegisterPendingIntent(publicKeyCredentialCreationOptions)
        return task.await()
    }

    suspend fun registerWithCredentialManager(context: Context) {
        val credentialManager = CredentialManager.create(context)
        val requestJson = """
{
  "rp": { "id": "$domain", "name": "Passkey Test" },
  "user": {
    "id": "VGVzdCBVc2Vy",
    "name": "test.user",
    "displayName": "$userName"
  },
  "challenge": "$challenge",
  "pubKeyCredParams": [
    {
      "type": "public-key",
      "alg": -7
    },
    {
      "type": "public-key",
      "alg": -257
    }
  ],
  "timeout": 1800000,
  "attestation": "none",
  "excludeCredentials": [],
  "authenticatorSelection": {
    "authenticatorAttachment": "platform",
    "requireResidentKey": true,
    "residentKey": "required",
    "userVerification": "required"
  }
}
        """.trimIndent()

        val request = CreatePublicKeyCredentialRequest(
            requestJson = requestJson,
            preferImmediatelyAvailableCredentials = false
        )

        val result = credentialManager.createCredential(context, request)

        println(result.data)
    }

    suspend fun signInWithCredentialManager(context: Context) {
        val credentialManager = CredentialManager.create(context)
        val requestJson = """
{
  "challenge": "$challenge",
  "allowCredentials": [
    {
      "type": "public-key",
      "id": "$base64EncodedCredentialId"
    }
  ],
  "timeout": 1800000,
  "userVerification": "required",
  "rpId": "$domain"
}
        """.trimIndent()
        val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
            requestJson = requestJson
        )
        val getCredRequest = GetCredentialRequest(
            listOf(getPublicKeyCredentialOption)
        )

        try {
            val result = credentialManager.getCredential(context, getCredRequest)
            println(result)
        } catch (error: NoCredentialException) {
            println("No credentials found.")
        }
    }

    suspend fun signIn(context: Context): PendingIntent {
        val publicKeyCredentialRequestOptions =
            PublicKeyCredentialRequestOptions.Builder().apply {
                setRpId(domain)
                setChallenge(challenge.toByteArray())
                setAllowList(
                    listOf(
                        PublicKeyCredentialDescriptor(
                            PublicKeyCredentialType.PUBLIC_KEY.toString(),
                            Base64Utils.decode(base64EncodedCredentialId),
                            listOf(
                                Transport.INTERNAL
                            )
                        )
                    )
                )
                setTimeoutSeconds(60.toDouble())
            }.build()

        val fidoClient = Fido.getFido2ApiClient(context)
        val task = fidoClient.getSignPendingIntent(publicKeyCredentialRequestOptions)
        return task.await()
    }
}