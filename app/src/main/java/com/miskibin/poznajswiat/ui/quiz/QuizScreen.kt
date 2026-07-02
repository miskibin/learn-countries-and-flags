package com.miskibin.poznajswiat.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miskibin.poznajswiat.R
import com.miskibin.poznajswiat.data.QuizMode
import com.miskibin.poznajswiat.ui.CorrectGreen
import com.miskibin.poznajswiat.ui.FlagImage
import com.miskibin.poznajswiat.ui.ScoreRing
import com.miskibin.poznajswiat.ui.WrongRed
import com.miskibin.poznajswiat.ui.history.TimelineBar
import com.miskibin.poznajswiat.ui.map.WorldMapView
import com.miskibin.poznajswiat.ui.rememberAnswerColors

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
            Spacer(Modifier.height(12.dp))
            if (q.kind == QuizMode.MAP) {
                MapQuestionContent(viewModel, state, q)
            } else {
                OptionsQuestionContent(viewModel, state, q)
            }
        }
    }
}

@Composable
private fun OptionsQuestionContent(
    viewModel: QuizViewModel,
    state: QuizUiState,
    q: Question,
) {
    val data = state.data ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        when (q.kind) {
            QuizMode.FLAGS -> if (q.reverse) {
                QuestionTitle(stringResource(R.string.quiz_question_flag_reverse))
                Spacer(Modifier.height(8.dp))
                Text(
                    q.target!!.namePl,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                QuestionTitle(stringResource(R.string.quiz_question_flags))
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FlagImage(
                        resId = data.flagRes[q.target!!.cca2] ?: 0,
                        contentDescription = null,
                        modifier = Modifier.width(240.dp),
                        corner = 14.dp,
                    )
                }
            }

            QuizMode.CAPITALS -> {
                QuestionTitle(stringResource(R.string.quiz_question_capitals))
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FlagImage(
                        resId = data.flagRes[q.target!!.cca2] ?: 0,
                        contentDescription = null,
                        modifier = Modifier.width(72.dp),
                        corner = 8.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(q.target.namePl, style = MaterialTheme.typography.headlineSmall)
                }
            }

            else -> { // HISTORY
                QuestionTitle(stringResource(R.string.quiz_question_history))
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            q.event!!.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        if (q.kind == QuizMode.FLAGS && q.reverse) {
            FlagGridOptions(viewModel, state, q)
        } else {
            val options: List<Pair<String, String>> = when (q.kind) {
                QuizMode.HISTORY -> q.yearOptions.map {
                    it.toString() to if (it < 0) "${-it} p.n.e." else it.toString()
                }
                QuizMode.CAPITALS -> q.options.map { it.cca2 to it.capitalPl }
                else -> q.options.map { it.cca2 to it.namePl }
            }
            options.forEach { (key, label) ->
                OptionCard(
                    label = label,
                    isTarget = key == q.correctKey,
                    isSelected = state.selected == key,
                    answered = state.answered,
                    onClick = { viewModel.answer(key) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.answered,
            enter = fadeIn() + slideInVertically { it / 3 },
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                FactCard(fact = q.target?.fact ?: q.event?.desc.orEmpty())
                if (q.kind == QuizMode.HISTORY && q.event != null) {
                    Spacer(Modifier.height(12.dp))
                    TimelineBar(event = q.event, allEvents = data.events)
                }
                Spacer(Modifier.height(12.dp))
                NextButton(viewModel, state)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Reverse flag question: four flags in a 2x2 grid, pick the right one. */
@Composable
private fun FlagGridOptions(
    viewModel: QuizViewModel,
    state: QuizUiState,
    q: Question,
) {
    val data = state.data ?: return
    val answers = rememberAnswerColors()
    q.options.chunked(2).forEach { rowOptions ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rowOptions.forEach { option ->
                val isTarget = option.cca2 == q.correctKey
                val isSelected = state.selected == option.cca2
                val borderColor = when {
                    state.answered && isTarget -> answers.correctAccent
                    state.answered && isSelected -> answers.wrongAccent
                    else -> Color.Transparent
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(3.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable(enabled = !state.answered) {
                            viewModel.answer(option.cca2)
                        }
                        .padding(6.dp)
                        .testTag("quiz_option"),
                    contentAlignment = Alignment.Center,
                ) {
                    FlagImage(
                        resId = data.flagRes[option.cca2] ?: 0,
                        contentDescription = option.namePl,
                        modifier = Modifier.fillMaxWidth(),
                        corner = 10.dp,
                    )
                }
            }
            if (rowOptions.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuestionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OptionCard(
    label: String,
    isTarget: Boolean,
    isSelected: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
) {
    val answers = rememberAnswerColors()
    val container by animateColorAsState(
        targetValue = when {
            answered && isTarget -> answers.correctContainer
            answered && isSelected -> answers.wrongContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "optionColor",
    )
    val content = when {
        answered && isTarget -> answers.onCorrect
        answered && isSelected -> answers.onWrong
        answered -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick,
        enabled = !answered,
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
            if (answered && isTarget) {
                Icon(Icons.Default.CheckCircle, null, tint = content)
            } else if (answered && isSelected && !isTarget) {
                Icon(Icons.Default.Cancel, null, tint = content)
            }
        }
    }
}

@Composable
private fun NextButton(viewModel: QuizViewModel, state: QuizUiState, compact: Boolean = false) {
    Button(
        onClick = { viewModel.next() },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 48.dp else 52.dp),
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

@Composable
private fun MapQuestionContent(
    viewModel: QuizViewModel,
    state: QuizUiState,
    q: Question,
) {
    val data = state.data ?: return
    val target = q.target ?: return

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
            Text(target.namePl, style = MaterialTheme.typography.headlineSmall)
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

    Box(Modifier.fillMaxSize()) {
        WorldMapView(
            map = data.worldMap,
            interactive = true,
            fillFor = { code ->
                when {
                    state.answered && code == target.cca2 -> CorrectGreen
                    code in state.wrongTaps -> WrongRed.copy(alpha = 0.75f)
                    else -> null
                }
            },
            onTap = { code -> viewModel.mapTap(code) },
            focusOn = if (state.revealed) target else null,
            resetKey = state.index,
        )

        if (!state.answered) {
            OutlinedButton(
                onClick = { viewModel.reveal() },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.quiz_reveal))
            }
        }

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
                val wrongName = state.lastWrongTap?.let { data.byCode[it]?.namePl }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = if (wrongName != null) {
                            stringResource(R.string.quiz_map_wrong_tap, wrongName)
                        } else {
                            stringResource(R.string.quiz_question_map) + " " + target.namePl
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
                                resId = data.flagRes[target.cca2] ?: 0,
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
                        FactCard(fact = target.fact)
                        Spacer(Modifier.height(8.dp))
                        NextButton(viewModel, state, compact = true)
                    }
                }
            }
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
                val q = result.question
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (q.target != null) {
                        FlagImage(
                            resId = data?.flagRes?.get(q.target.cca2) ?: 0,
                            contentDescription = null,
                            modifier = Modifier.width(48.dp),
                            corner = 6.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            q.target.namePl,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            q.event!!.yearLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            q.event.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }
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
