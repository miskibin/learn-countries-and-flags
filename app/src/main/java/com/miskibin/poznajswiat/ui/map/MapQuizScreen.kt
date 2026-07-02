package com.miskibin.poznajswiat.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.ui.CorrectGreen
import com.miskibin.poznajswiat.ui.FlagImage
import com.miskibin.poznajswiat.ui.WrongRed
import com.miskibin.poznajswiat.ui.quiz.FactCard
import com.miskibin.poznajswiat.ui.quiz.QuizViewModel
import com.miskibin.poznajswiat.ui.quiz.ResultsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapQuizScreen(
    viewModel: QuizViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    if (state.finished) {
        ResultsScreen(state = state, onReplay = { viewModel.startSession() }, onHome = onExit)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.quiz_progress,
                            state.index + 1,
                            state.questions.size.coerceAtLeast(1),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
            )
        },
    ) { padding ->
        val q = state.current
        val data = state.data
        if (state.loading || q == null || data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            LinearProgressIndicator(
                progress = { state.index.toFloat() / state.questions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.quiz_question_map),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(q.target.namePl, style = MaterialTheme.typography.headlineSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = if (i < state.triesLeft) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (i < state.triesLeft) WrongRed
                            else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            Box(Modifier.weight(1f)) {
                WorldMapView(
                    map = data.worldMap,
                    countries = data.countries,
                    interactive = true,
                    fillFor = { code ->
                        when {
                            state.answered && code == q.target.cca2 -> CorrectGreen
                            code in state.wrongTaps -> WrongRed.copy(alpha = 0.75f)
                            else -> null
                        }
                    },
                    onTap = { code -> viewModel.mapTap(code) },
                )

                // Feedback overlays
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                ) {
                    AnimatedVisibility(
                        visible = !state.answered && state.lastWrongTap != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 },
                    ) {
                        val wrongName = state.lastWrongTap
                            ?.let { data.byCode[it]?.namePl }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Text(
                                text = if (wrongName != null) {
                                    stringResource(R.string.quiz_map_wrong_tap, wrongName)
                                } else {
                                    stringResource(R.string.quiz_question_map) + " " + q.target.namePl
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = state.answered,
                        enter = fadeIn() + slideInVertically { it / 2 },
                    ) {
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FlagImage(
                                        resId = data.flagRes[q.target.cca2] ?: 0,
                                        contentDescription = null,
                                        modifier = Modifier.width(56.dp),
                                        corner = 6.dp,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (state.revealed) {
                                            stringResource(R.string.quiz_map_revealed)
                                        } else {
                                            stringResource(R.string.quiz_correct)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (state.revealed) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else CorrectGreen,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                FactCard(fact = q.target.fact)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.next() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        stringResource(
                                            if (state.index + 1 >= state.questions.size) {
                                                R.string.quiz_finish
                                            } else R.string.quiz_next
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
