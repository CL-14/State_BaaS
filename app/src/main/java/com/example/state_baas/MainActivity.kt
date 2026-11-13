package com.example.state_baas

import android.os.Bundle                            //bundle is the key value container Android uses to pass/save state
import androidx.activity.ComponentActivity          //base class for an Activity (a single screen) in Compose apps
import androidx.activity.compose.setContent         // setContent attaches Compose UI to the Activity
import androidx.compose.foundation.layout.Column    // Column stacks children vertically
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable          //// marks a function as UI building
import androidx.compose.runtime.getValue            // getValue/setValue are required for the `by` delegate syntax
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf   // creates observable Int state
import androidx.compose.runtime.saveable.rememberSaveable // like remember, but survives configuration changes
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.state_baas.ui.theme.State_BaaSTheme

class MainActivity : ComponentActivity() {

    // CLASS-LEVEL state (Counter A). Declared in the class, not inside a composable
    // mutableIntStateOf makes it observable, so when it changes, composables reading it recompose
    // Because the Activity object is destroyed and rebuilt on rotation, this resets to 0 then
    private var plainCounter: Int by mutableIntStateOf(0)

    // onCreate runs when the Activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)      // let the parent class do its own setup first
        setContent {                            // everything inside here is Compose UI
            MaterialTheme {                     // applies Material Design styling to children
                CounterScreen(
                    plainCounter = plainCounter,           // state goes DOWN as a parameter
                    onPlainIncrement = { plainCounter++ }  // events come UP as a lambda that modifies the class-level state
                )
            }
        }
    }
}

// Parent ("stateful") composable: it owns Counter B and wires everything together.
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

// Stateless composable: holds no state, only displays what it is given and reports clicks.
// Easy to reuse and test, which is the point of hoisting.
@Composable
fun CounterContent(
    plainCount: Int,
    savedCount: Int,
    onIncrementBoth: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center // centre children vertically
    ) {
        Text("Plain counter (resets on rotate): $plainCount")      // $ inserts the variable into the string
        Text("Saveable counter (survives rotate): $savedCount")
        Button(onClick = onIncrementBoth) { // one button, both counters
            Text("Increment both")
        }
    }
}

//leaving commented for now
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