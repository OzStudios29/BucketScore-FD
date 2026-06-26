package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.BucketScoreViewModel

@Composable
fun MatchCodeRedirector(
    matchCode: String,
    viewModel: BucketScoreViewModel,
    onRedirect: (Long) -> Unit,
    onFail: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(matchCode) {
        val match = viewModel.getMatchByPublicCode(matchCode)
        if (match != null) {
            onRedirect(match.id)
        } else {
            val numericPart = matchCode.replace("BS-", "", ignoreCase = true).trim()
            val matchId = numericPart.toLongOrNull()
            if (matchId != null) {
                val matchById = viewModel.getMatchById(matchId)
                if (matchById != null) {
                    onRedirect(matchId)
                    return@LaunchedEffect
                }
            }
            Toast.makeText(context, "Match code not found.", Toast.LENGTH_LONG).show()
            onFail()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
