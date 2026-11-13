package com.example.state_baas

import android.os.Bundle                            //bundle is the key value container Android uses to pass/save state
import androidx.activity.ComponentActivity          //base class for an Activity (a single screen) in Compose apps
import androidx.activity.compose.setContent         // setContent attaches Compose UI to the Activity
import androidx.compose.foundation.layout.Column    // Column stacks children vertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable          //// marks a function as UI building
import androidx.compose.runtime.getValue            // getValue/setValue are required for the `by` delegate syntax
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf   // creates observable Int state
import androidx.compose.runtime.saveable.rememberSaveable // like remember, but survives configuration changes
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



//firebase support imports
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.android.gms.tasks.Task               // the "result arrives later" object
import com.google.firebase.auth.AuthResult              // what a sign-in Task delivers
import com.google.firebase.auth.FirebaseAuth            // entry point to Firebase Authentication
import com.google.firebase.auth.FirebaseUser            // represents a signed-in user

class MainActivity : ComponentActivity() {

    // CLASS-LEVEL state (Counter A). Declared in the class, not inside a composable
    // mutableIntStateOf makes it observable, so when it changes, composables reading it recompose
    // Because the Activity object is destroyed and rebuilt on rotation, this resets to 0 then
    private var plainCounter: Int by mutableIntStateOf(0)


    // ---------------------- this block will cover firebase ----------------------

    // The Firebase Authentication entry point. getInstance() returns the shared instance for this app
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // who is signed in? FirebaseUser? means "a user OR null" (null = nobody).
    // initialize from auth.currentUser because Firebase remembers the session on the device
    private var currentUser: FirebaseUser? by mutableStateOf(auth.currentUser)

    // the latest error to show on the login screen (null = no error)
    private var authError: String? by mutableStateOf(null)

    // Creates a new account, then signs in automatically on success
    private fun register(email: String, password: String) {
        try {
            // Validate first with require()
            require(email.isNotBlank()) { "Email must not be empty" }
            require(password.length >= 10) { "Password must be at least 10 characters" }

            auth.createUserWithEmailAndPassword(email, password)
                // runs later, when Firebase's network call finishes
                .addOnCompleteListener(this) { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        currentUser = auth.currentUser // changing this state triggers recomposition -> counter screen appears
                        authError = null               // clear any old error
                    } else {
                        // task.exception explains what went wrong
                        authError = task.exception?.message ?: "Registration failed"
                        Log.w("Auth", "register failed", task.exception)
                    }
                }
        } catch (e: IllegalArgumentException) {
            authError = e.message // show our require() message on screen
        }
    }

    // Signs in an existing account
    private fun signIn(email: String, password: String) {
        try {
            require(email.isNotBlank()) { "Email must not be empty" }
            require(password.isNotBlank()) { "Password must not be empty" }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        currentUser = auth.currentUser
                        authError = null
                    } else {
                        authError = task.exception?.message ?: "Sign-in failed"
                        Log.w("Auth", "signIn failed", task.exception)
                    }
                }
        } catch (e: IllegalArgumentException) {
            authError = e.message
        }
    }

    // Signs out locally and clears our state so the login screen returns.
    private fun signOut() {
        auth.signOut()
        currentUser = null
    }
// End of firebase block


    // onCreate runs when the Activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)      // let the parent class do its own setup first
        runPreconditionDemos()      // runs the check()/require() demos; read results in Logcat
        runCustomExceptionDemo()    // runs the custom exception demo
        setContent {                            // everything inside here is Compose UI
            MaterialTheme {                     // applies Material Design styling to children
            // currentUser is state, so this if/else re evaluates whenever it changes
                if (currentUser === null)
                {
                    //added the firebase auth screen since it needs to show up before the counter

                    //Nobody signed in -> login screen. Events go up as lambdas
                    AuthScreen(
                        errorMessage = authError,
                        onSignIn = { email: String, password: String -> signIn(email, password) },
                        onRegister = { email: String, password: String -> register(email, password) }
                    )
                } else {
                    Column {
                        Text("Signed in as ${currentUser?.email}") // ?. = only read email if currentUser isn't null
                        Button(onClick = { signOut() }) { Text("Sign out") }
                        CounterScreen(
                            plainCounter = plainCounter,     // state goes down as a parameter
                            onPlainIncrement = { plainCounter++ }   // events come up as a lambda that modifies the class-level state
                        )
                    }
                }

            }
        }
    }
}



// this owns Counter B and wires everything together
@Composable
fun CounterScreen(
    plainCounter: Int,            // Counter A's value, owned by MainActivity
    onPlainIncrement: () -> Unit  // function that increments Counter A (Unit = "returns nothing")
) {
    // Counter B: rememberSaveable stores the value in the saved-state Bundle,
    // so it survives rotation. `by` lets us use savedCounter as a plain Int.
    var savedCounter: Int by rememberSaveable { mutableIntStateOf(0) }

    // Hand the values and ONE event lambda to the stateless child.
    CounterContent(
        plainCount = plainCounter,
        savedCount = savedCounter,
        onIncrementBoth = {
            onPlainIncrement() // bump Counter A (lives in the Activity)
            savedCounter++     // bump Counter B (lives here)
        }
    )
}

// holds no state, only displays what it is given and reports clicks
@Composable
fun CounterContent(
    plainCount: Int,
    savedCount: Int,
    onIncrementBoth: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center // center children vertically
    ) {
        Text("Plain counter (resets on rotate): $plainCount")      //inserts the variable into the string
        Text("Saveable counter (survives rotate): $savedCount")
        Button(onClick = onIncrementBoth) { // one button, both counters
            Text("Increment both")
        }
    }
}

// Login/register form. It owns only the text being typed, results go up via callbacks
@Composable
fun AuthScreen(
    errorMessage: String?,                           // error to display, or null
    onSignIn: (String, String) -> Unit,              // called with (email, password)
    onRegister: (String, String) -> Unit
) {
    // Email survives rotation via rememberSaveable
    var email: String by rememberSaveable { mutableStateOf("") }
    // password uses plain remember on purpose, so it isn't written into the saved-state bundle

    // rotating clears the password field.
    var password: String by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        OutlinedTextField(
            value = email,
            onValueChange = { newText: String -> email = newText }, // text fields only change when you update their state
            label = { Text("Email") },
            singleLine = true
        )
        OutlinedTextField(
            value = password,
            onValueChange = { newText: String -> password = newText },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation() // shows dots instead of characters for the password
        )
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error) // only drawn when there is an error
        }
        Row {
            Button(onClick = { onSignIn(email, password) }) { Text("Sign in") }
            Button(onClick = { onRegister(email, password) }) { Text("Register") }
        }
    }
}

//leaving default main act code commented for now
//
//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    State_BaaSTheme {
//        Greeting("Android")
//    }
//}