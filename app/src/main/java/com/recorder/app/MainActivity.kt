package com.recorder.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.recorder.app.ui.navigation.RecorderApp
import com.recorder.app.ui.theme.RecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    private var isSignedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        isSignedIn = GoogleSignIn.getLastSignedInAccount(this) != null

        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null) {
                    isSignedIn = true
                    // Recompose so navigation reacts to the signed-in state
                    setContent {
                        RecorderTheme {
                            RecorderApp(
                                signIn = ::signIn,
                                googleSignInClient = googleSignInClient,
                                isSignedIn = true
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Sign-in failed — stay on sign-in screen
            }
        }

        setContent {
            RecorderTheme {
                RecorderApp(
                    signIn = ::signIn,
                    googleSignInClient = googleSignInClient,
                    isSignedIn = isSignedIn
                )
            }
        }
    }

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }
}
