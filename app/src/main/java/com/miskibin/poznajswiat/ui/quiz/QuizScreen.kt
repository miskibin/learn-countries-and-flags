package com.miskibin.poznajswiat.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.ui.CorrectGreen
import com.miskibin.poznajswiat.ui.CorrectGreenContainer
import com.miskibin.poznajswiat.ui.FlagImage
import com.miskibin.poznajswiat.ui.ScoreRing
import com.miskibin.poznajswiat.ui.WrongRed
import com.miskibin.poznajswiat.ui.WrongRedContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    if (state.finished) {
        ResultsScreen(
            state = state,
            onReplay = { viewModel.startSession() },
            onHome = onExit,
        )
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
        if (state.loading || q == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val data = state.data ?: return@Scaffold

        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            LinearProgressIndicator(
                progress = { state.index.toFloat() / state.questions.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            if (viewModel.mode == QuizMode.FLAGS) {
                Text(
                    stringResource(R.string.quiz_question_flags),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FlagImage(
                        resId = data.flagRes[q.target.cca2] ?: 0,
                        contentDescription = null,
                        modifier = Modifier.width(240.dp),
                        corner = 14.dp,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.quiz_question_capitals),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlagImage(
                        resId = data.flagRes[q.target.cca2] ?: 0,
                        contentDescription = null,
                        modifier = Modifier.width(72.dp),
                        corner = 8.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(q.target.namePl, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            q.options.forEach { option ->
                val label =
                    if (viewModel.mode == QuizMode.CAPITALS) option.capitalPl else option.namePl
                val isTarget = option.cca2 == q.target.cca2
                val isSelected = state.selected == option.cca2
                val container by animateColorAsState(
                    targetValue = when {
                        state.answered && isTarget -> CorrectGreenContainer
                        state.answered && isSelected -> WrongRedContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(300),
                    label = "optionColor",
                )
                val content = when {
                    state.answered && isTarget -> Color(0xFF0A3D0C)
                    state.answered && isSelected -> Color(0xFF4E0F0D)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = container),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { viewModel.answer(option.cca2) },
                    enabled = !state.answered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("quiz_option"),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium,
                            color = content,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.answered && isTarget) {
                            Icon(Icons.Default.CheckCircle, null, tint = CorrectGreen)
                        } else if (state.answered && isSelected && !isTarget) {
                            Icon(Icons.Default.Cancel, null, tint = WrongRed)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.answered,
                enter = fadeIn() + slideInVertically { it / 3 },
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    FactCard(fact = q.target.fact)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.next() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.index + 1 >= state.questions.size) R.string.quiz_finish
                                else R.string.quiz_next
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FactCard(fact: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = stringResource(R.string.quiz_fact_label),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                fact,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
fun ResultsScreen(
    state: QuizUiState,
    onReplay: () -> Unit,
    onHome: () -> Unit,
) {
    val total = state.questions.size
    val score = state.score
    val message = when {
        total > 0 && score == total -> stringResource(R.string.results_perfect)
        total > 0 && score >= total * 3 / 4 -> stringResource(R.string.results_great)
        total > 0 && score >= total / 2 -> stringResource(R.string.results_good)
        else -> stringResource(R.string.results_keep_trying)
    }
    val data = state.data

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.results_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))
        ScoreRing(
            fraction = if (total == 0) 0f else score.toFloat() / total,
            modifier = Modifier.size(150.dp),
            ringWidth = 14.dp,
            color = if (total > 0 && score >= total / 2) CorrectGreen else MaterialTheme.colorScheme.primary,
        ) {
            Text(
                stringResource(R.string.results_score, score, total),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(state.results) { result ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlagImage(
                        resId = data?.flagRes?.get(result.country.cca2) ?: 0,
                        contentDescription = null,
                        modifier = Modifier.width(48.dp),
                        corner = 6.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        result.country.namePl,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (result.correct) {
                        Icon(Icons.Default.CheckCircle, null, tint = CorrectGreen)
                    } else {
                        Icon(Icons.Default.Cancel, null, tint = WrongRed)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onReplay,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.results_play_again), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.results_home), style = MaterialTheme.typography.titleMedium)
        }
    }
}
