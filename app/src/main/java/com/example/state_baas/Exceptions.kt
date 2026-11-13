package com.example.state_baas

import android.util.Log // Log writes messages to logcat

private const val TAG: String = "ExceptionDemo" // label to filter Logcat by

// check(): validates STATE. Throws IllegalStateException if the Boolean is false
fun useLight(isLightOn: Boolean) {
    check(isLightOn) { "The light is not turned on" }
    Log.d(TAG, "Light is on, carrying on")
}

// require(): validates an ARGUMENT. Throws IllegalArgumentException if the condition is false
fun processNumber(number: Int) {
    require(number >= 0) { "Number must be non-negative, but was $number" }
    Log.d(TAG, "Number $number is fine")
}

fun runPreconditionDemos() {
    // The try block holds code that might throw
    try {
        useLight(isLightOn = false) // provokes check() -> IllegalStateException
        processNumber(number = -5)  // never reached: the exception above skips the rest of the try block
    }
    // Each catch handles one exception type. Specific types go first
    catch (e: IllegalStateException) {
        Log.e(TAG, "check() failed (IllegalStateException): ${e.message}")
    }
    catch (e: IllegalArgumentException) {
        Log.e(TAG, "require() failed (IllegalArgumentException): ${e.message}")
    }

    // Separate try so the require() case actually runs too
    try {
        processNumber(number = -5) // provokes require() -> IllegalArgumentException
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "require() failed (IllegalArgumentException): ${e.message}")
    }
}

// open allows other classes to inherit from this one
// Exception(message) means "extends Exception", passing the message to the parent
open class WordTooLongException(
    val word: String,                       // val in the constructor makes this a stored, readable property
    message: String = "Word is longer than 10 characters"
) : Exception(message) {

    // Overriding toString() makes it obvious in the log that this custom exception fired
    override fun toString(): String =
        "WordTooLongException(word='$word', length=${word.length}): $message"
}

// a more specific exception inheriting from the existing one
class SuperMegaLongWordException(word: String) :
    WordTooLongException(word, "your word is so long you threw a Super, duper, extra, ultra, hyper, maxi, multi, mega exception (over 20 characters), yikes")

// Checks a word and provokes the exception if it is too long
fun checkWord(word: String) {
    if (word.length > 20) throw SuperMegaLongWordException(word) // subclass thrown
    if (word.length > 10) throw WordTooLongException(word)      // the custom exception thrown
    Log.d("ExceptionDemo", "'$word' is an acceptable length")
}

fun runCustomExceptionDemo() {
    try {
        checkWord("Hippopotomonstrosesquippedaliophobia") // throws the SuperMegaLongWordException
    } catch (e: SuperMegaLongWordException) {
        // Subclass first, otherwise the parent's catch would swallow it
        Log.e("ExceptionDemo", "Caught subclass: $e")
    } catch (e: WordTooLongException) {
        Log.e("ExceptionDemo", "Caught custom exception: $e")
    }
}